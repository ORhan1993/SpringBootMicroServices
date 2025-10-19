package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bozgeyik.paymentservice.client.NotificationClient;
import org.bozgeyik.paymentservice.client.dto.NotificationRequest;
import org.bozgeyik.paymentservice.dto.DepositRequest;
import org.bozgeyik.paymentservice.dto.FxRequest;
import org.bozgeyik.paymentservice.dto.TransferRequest;
import org.bozgeyik.paymentservice.dto.WithdrawRequest;
import org.bozgeyik.paymentservice.event.TransactionCompletedEvent; // E-posta olayı için
import org.bozgeyik.paymentservice.exception.IdempotencyException;
import org.bozgeyik.paymentservice.exception.InsufficientFundsException;
import org.bozgeyik.paymentservice.model.*;
import org.bozgeyik.paymentservice.repository.TransactionRepository;
import org.springframework.context.ApplicationEventPublisher; // Olay fırlatıcı
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrchestratorService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final LedgerService ledgerService;
    private final FXService fxService;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher; // E-posta olayı için

    /**
     * Tekrarlanan istekleri engellemek için Idempotency anahtarını kontrol eder.
     */
    private void checkIdempotency(String key) {
        if (transactionRepository.existsByIdempotencyKey(key)) {
            throw new IdempotencyException("Bu işlem daha önce gerçekleştirildi: " + key);
        }
    }

    /**
     * Dışarıdan para yükleme (On-Ramp).
     * Önce dış ödeme sistemi (Stripe vb.) çağrılır, başarılıysa bakiye güncellenir.
     */
    @Transactional
    public Transaction depositOnRamp(DepositRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        Wallet wallet = walletService.getWalletByCustomerId(request.getCustomerId());

        // --- DIŞ ÖDEME SİSTEMİ ÇAĞRISI (SİMÜLASYON) ---
        // boolean paymentSuccess = paymentGateway.charge(request.getPaymentGatewayToken(), request.getAmount());
        // if (!paymentSuccess) {
        //    createTransactionEntry(...) // FAILED
        //    throw new RuntimeException("Ödeme alınamadı");
        // }
        // --- BİTTİ ---

        Transaction transaction;
        try {
            // Cüzdan bakiyesini artır (Atomik işlem)
            ledgerService.updateBalance(wallet.getId(), request.getCurrency(), request.getAmount());

            // Başarılı işlem kaydı
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), null, wallet.getId(),
                    request.getAmount(), request.getCurrency(),
                    request.getAmount(), request.getCurrency(), BigDecimal.ONE,
                    request.getDescription(), TransactionType.DEPOSIT, TransactionStatus.COMPLETED
            );

            // Anlık (Push/SMS) bildirim
            sendNotification(wallet.getCustomerId(),
                    String.format("%.2f %s cüzdanınıza yüklendi.", request.getAmount(), request.getCurrency()),
                    "DEPOSIT_COMPLETED");

            // ASENKRON E-POSTA BİLDİRİMİ: İşlem commit edilince tetiklenir
            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));

        } catch (Exception e) {
            log.error("Deposit hatası: {}", e.getMessage());
            // Başarısız işlem kaydı
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), null, wallet.getId(),
                    request.getAmount(), request.getCurrency(),
                    request.getAmount(), request.getCurrency(), BigDecimal.ONE,
                    "BAŞARISIZ: " + e.getMessage(), TransactionType.DEPOSIT, TransactionStatus.FAILED
            );
            throw new RuntimeException("Deposit işlemi sırasında bakiye güncellenemedi.", e);
        }

        return transaction;
    }

    /**
     * Dışarıya para çekme (Off-Ramp).
     * Önce bakiye düşülür, başarılıysa Banka/SWIFT API'ı çağrılır.
     * (SAGA Pattern gerektirebilir)
     */
    @Transactional
    public Transaction withdrawOffRamp(WithdrawRequest request) throws InsufficientFundsException {
        checkIdempotency(request.getIdempotencyKey());

        Wallet wallet = walletService.getWalletByCustomerId(request.getCustomerId());
        Transaction transaction;

        try {
            // 1. Önce bakiyeyi düş (Atomik işlem)
            ledgerService.updateBalance(wallet.getId(), request.getCurrency(), request.getAmount().negate());

            // 2. DIŞ BANKA/SWIFT SİSTEMİ ÇAĞRISI (SİMÜLASYON)
            // boolean bankTransferSuccess = swiftClient.send(request.getIban(), request.getAmount(), request.getCurrency());
            // if (!bankTransferSuccess) {
            //    // Hata! Para düştü ama yollanamadı. (SAGA telafi işlemi gerektirir)
            //    throw new RuntimeException("Banka transferi başarısız.");
            // }
            // --- BİTTİ ---

            // Başarılı işlem kaydı
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), wallet.getId(), null,
                    request.getAmount(), request.getCurrency(),
                    request.getAmount(), request.getCurrency(), BigDecimal.ONE,
                    request.getDescription(), TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED
            );

            // Anlık bildirim
            sendNotification(wallet.getCustomerId(),
                    String.format("%.2f %s cüzdanınızdan çekildi.", request.getAmount(), request.getCurrency()),
                    "WITHDRAWAL_COMPLETED");

            // ASENKRON E-POSTA BİLDİRİMİ
            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));

        } catch (InsufficientFundsException e) {
            log.warn("Withdraw hatası (Bakiye Yetersiz): {}", e.getMessage());
            // Bakiye yetersizse başarısız kayıt at
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), wallet.getId(), null,
                    request.getAmount(), request.getCurrency(),
                    request.getAmount(), request.getCurrency(), BigDecimal.ONE,
                    "BAŞARISIZ: Yetersiz Bakiye", TransactionType.WITHDRAWAL, TransactionStatus.FAILED
            );
            throw e; // Controller'a 400 Bad Request dönsün
        } catch (Exception e) {
            log.error("Withdraw hatası: {}", e.getMessage());
            // Başarısız kayıt at (Bakiye düşmüş olabilir, manuel inceleme gerekebilir)
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), wallet.getId(), null,
                    request.getAmount(), request.getCurrency(),
                    request.getAmount(), request.getCurrency(), BigDecimal.ONE,
                    "BAŞARISIZ: " + e.getMessage(), TransactionType.WITHDRAWAL, TransactionStatus.FAILED
            );
            throw new RuntimeException("Withdraw işlemi sırasında hata oluştu.", e);
        }

        return transaction;
    }

    /**
     * Cüzdanlar arası transfer (FX destekli).
     * Bu metot ATOMİKTİR. Ya hep ya hiç.
     */
    @Transactional
    public Transaction transferMoney(TransferRequest request)
            throws InsufficientFundsException, EntityNotFoundException {

        checkIdempotency(request.getIdempotencyKey());

        Wallet fromWallet = walletService.getWalletByCustomerId(request.getFromCustomerId());
        Wallet toWallet = walletService.getWalletByCustomerId(request.getToCustomerId());

        String fromCurrency = request.getCurrency();
        String toCurrency = request.getTargetCurrency();
        BigDecimal fromAmount = request.getAmount();
        BigDecimal convertedAmount;
        BigDecimal rate = BigDecimal.ONE;

        // Kur çevrimi (FX) gerekliyse hesapla
        if (!fromCurrency.equals(toCurrency)) {
            log.info("FX Transferi: {} {} -> {}", fromAmount, fromCurrency, toCurrency);
            rate = fxService.getRate(fromCurrency, toCurrency);
            convertedAmount = fxService.convert(fromAmount, fromCurrency, toCurrency);
        } else {
            convertedAmount = fromAmount;
        }

        Transaction transaction;
        try {
            // ATOMİK BLOK BAŞLANGICI (Tüm bakiye işlemleri kilitli)
            // 1. Para Çıkışı (DEBIT)
            ledgerService.updateBalance(fromWallet.getId(), fromCurrency, fromAmount.negate());

            // 2. Para Girişi (CREDIT)
            ledgerService.updateBalance(toWallet.getId(), toCurrency, convertedAmount);
            // ATOMİK BLOK SONU

            // 3. Başarılı İşlem Kaydı
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), fromWallet.getId(), toWallet.getId(),
                    fromAmount, fromCurrency, convertedAmount, toCurrency, rate,
                    request.getDescription(), TransactionType.TRANSFER, TransactionStatus.COMPLETED
            );

            // 4. Anlık Bildirimler
            sendNotification(fromWallet.getCustomerId(),
                    String.format("%.2f %s gönderdiniz. (Alıcı %.2f %s aldı)", fromAmount, fromCurrency, convertedAmount, toCurrency),
                    "TRANSFER_SENT");

            sendNotification(toWallet.getCustomerId(),
                    String.format("%.2f %s aldınız. (Gönderen %.2f %s yolladı)", convertedAmount, toCurrency, fromAmount, fromCurrency),
                    "TRANSFER_RECEIVED");

            // 5. ASENKRON E-POSTA BİLDİRİMİ
            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));

        } catch (Exception e) {
            // Hata durumunda (örn: Yetersiz Bakiye), @Transactional sayesinde
            // hem DEBIT hem de CREDIT işlemleri geri alınır (rollback).
            log.error("Transfer başarısız (Rollback): {}", e.getMessage());

            // Başarısız işlem kaydı
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), fromWallet.getId(), toWallet.getId(),
                    fromAmount, fromCurrency, convertedAmount, toCurrency, rate,
                    "BAŞARISIZ: " + e.getMessage(), TransactionType.TRANSFER, TransactionStatus.FAILED
            );

            // Orijinal hatayı Controller'a fırlat
            if (e instanceof InsufficientFundsException) throw (InsufficientFundsException) e;
            if (e instanceof EntityNotFoundException) throw (EntityNotFoundException) e;
            throw new RuntimeException("Transfer sırasında beklenmeyen hata", e);
        }

        return transaction;
    }

    /**
     * Cüzdan içi Döviz Alım/Satım (FX).
     * Bu metot ATOMİKTİR.
     */
    @Transactional
    public Transaction executeFx(FxRequest request) throws InsufficientFundsException, EntityNotFoundException {
        checkIdempotency(request.getIdempotencyKey());

        Wallet wallet = walletService.getWalletByCustomerId(request.getCustomerId());

        BigDecimal rate = fxService.getRate(request.getFromCurrency(), request.getToCurrency());
        BigDecimal convertedAmount = fxService.convert(request.getAmount(), request.getFromCurrency(), request.getToCurrency());

        Transaction transaction;
        try {
            // ATOMİK BLOK (Aynı cüzdan içinde iki farklı bakiye satırı kilitlenir)
            // 1. Para Çıkışı (Satılan birim)
            ledgerService.updateBalance(wallet.getId(), request.getFromCurrency(), request.getAmount().negate());

            // 2. Para Girişi (Alınan birim)
            ledgerService.updateBalance(wallet.getId(), request.getToCurrency(), convertedAmount);
            // ATOMİK BLOK SONU

            // Başarılı işlem kaydı
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), wallet.getId(), wallet.getId(),
                    request.getAmount(), request.getFromCurrency(), convertedAmount, request.getToCurrency(), rate,
                    "Döviz Alım/Satım", TransactionType.FX_TRADE, TransactionStatus.COMPLETED
            );

            // Anlık bildirim
            sendNotification(wallet.getCustomerId(),
                    String.format("%.2f %s sattınız, %.2f %s aldınız. Kur: %s",
                            request.getAmount(), request.getFromCurrency(), convertedAmount, request.getToCurrency(), rate),
                    "FX_TRADE_COMPLETED");

            // ASENKRON E-POSTA BİLDİRİMİ
            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));

        } catch (Exception e) {
            log.error("FX Trade başarısız (Rollback): {}", e.getMessage());
            // Başarısız işlem kaydı
            transaction = createTransactionEntry(
                    request.getIdempotencyKey(), wallet.getId(), wallet.getId(),
                    request.getAmount(), request.getFromCurrency(), convertedAmount, request.getToCurrency(), rate,
                    "BAŞARISIZ: " + e.getMessage(), TransactionType.FX_TRADE, TransactionStatus.FAILED
            );
            // Orijinal hatayı fırlat
            if (e instanceof InsufficientFundsException) throw (InsufficientFundsException) e;
            if (e instanceof EntityNotFoundException) throw (EntityNotFoundException) e;
            throw new RuntimeException("FX işlemi sırasında beklenmeyen hata", e);
        }

        return transaction;
    }

    /**
     * Müşterinin işlem dökümünü (Sayfalanmış) getirir.
     */
    public Page<Transaction> getTransactionsForCustomer(String customerId, Pageable pageable) {
        Wallet wallet = walletService.getWalletByCustomerId(customerId);
        return transactionRepository.findByFromWalletIdOrToWalletId(wallet.getId(), wallet.getId(), pageable);
    }

    // --- Yardımcı Metotlar ---

    /**
     * Veritabanına Transaction (Defter) kaydı oluşturur.
     */
    private Transaction createTransactionEntry(
            String key, Long fromId, Long toId,
            BigDecimal originAmount, String originCurrency,
            BigDecimal convertedAmount, String targetCurrency, BigDecimal rate,
            String desc, TransactionType type, TransactionStatus status) {

        Transaction tx = new Transaction();
        tx.setIdempotencyKey(key);
        tx.setFromWalletId(fromId);
        tx.setToWalletId(toId);
        tx.setOriginalAmount(originAmount);
        tx.setOriginalCurrency(originCurrency);
        tx.setConvertedAmount(convertedAmount);
        tx.setTargetCurrency(targetCurrency);
        tx.setExchangeRateUsed(rate);
        tx.setDescription(desc != null ? desc : type.name());
        tx.setTransactionType(type);
        tx.setStatus(status);
        return transactionRepository.save(tx);
    }

    /**
     * Anlık bildirim (Push, SMS vb.) gönderir.
     * Bu işlem ana transaction'ı etkilememelidir (try-catch).
     */
    private void sendNotification(String customerId, String message, String notificationType) {
        try {
            // Feign Client veya RestTemplate çağrısı
            notificationClient.sendNotification(new NotificationRequest(
                    customerId, message, notificationType
            ));
        } catch (Exception e) {
            // Bildirim hatası ana işlemi durdurmamalı. Sadece loglanır.
            log.error("Anlık bildirim gönderilemedi (Ana işlem etkilenmedi): Customer {}, Error: {}", customerId, e.getMessage());
        }
    }
}