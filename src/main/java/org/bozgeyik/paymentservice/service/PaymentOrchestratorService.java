package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bozgeyik.paymentservice.client.NotificationClient;
import org.bozgeyik.paymentservice.client.dto.NotificationRequest;
import org.bozgeyik.paymentservice.dto.*;
import org.bozgeyik.paymentservice.event.TransactionCompletedEvent;
import org.bozgeyik.paymentservice.exception.IdempotencyException;
import org.bozgeyik.paymentservice.exception.InsufficientFundsException;
import org.bozgeyik.paymentservice.model.*;
import org.bozgeyik.paymentservice.repository.TransactionRepository;
import org.bozgeyik.paymentservice.repository.WalletRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrchestratorService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final LedgerService ledgerService;
    private final FXService fxService;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final MockSwiftService mockSwiftService;
    // Komisyon oranı (%5) - Sabit olarak tanımlayalım
    private static final BigDecimal SWIFT_FEE_RATE = new BigDecimal("0.05");

    @Transactional
    public Transaction depositOnRamp(DepositRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        Wallet wallet = null;
        try {
            // Düzeltme: E-posta ve para birimine göre cüzdanı bul.
            wallet = walletService.getWalletByUserEmailAndCurrency(request.getCustomerId(), request.getCurrency());

            ledgerService.updateBalance(wallet.getId(), request.getCurrency(), request.getAmount());

            return handleSuccessfulDeposit(request, wallet);

        } catch (Exception e) {
            log.error("Deposit hatası (Rollback tetiklendi): {}", e.getMessage(), e);
            handleFailedTransaction(request, wallet, e);
            return null; 
        }
    }

    @Transactional
    public Transaction withdrawOffRamp(WithdrawRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        Wallet wallet = null;
        try {
            // Düzeltme: E-posta ve para birimine göre cüzdanı bul.
            wallet = walletService.getWalletByUserEmailAndCurrency(request.getCustomerId(), request.getCurrency());

            ledgerService.updateBalance(wallet.getId(), request.getCurrency(), request.getAmount().negate());

            return handleSuccessfulWithdrawal(request, wallet);

        } catch (Exception e) {
            log.error("Withdraw hatası (Rollback tetiklendi): {}", e.getMessage(), e);
            handleFailedTransaction(request, wallet, e);
            return null;
        }
    }

    @Transactional
    public Transaction transferMoney(TransferRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        String fromCurrency = request.getCurrency();
        String toCurrency = request.getTargetCurrency();
        BigDecimal fromAmount = request.getAmount();
        BigDecimal convertedAmount;
        BigDecimal rate = BigDecimal.ONE;

        if (!fromCurrency.equals(toCurrency)) {
            log.info("FX Transferi: {} {} -> {}", fromAmount, fromCurrency, toCurrency);
            rate = fxService.getRate(fromCurrency, toCurrency);
            convertedAmount = fxService.convert(fromAmount, fromCurrency, toCurrency);
        } else {
            convertedAmount = fromAmount;
        }

        Wallet fromWallet = null;
        Wallet toWallet = null;
        try {
            // Düzeltme: E-posta ve para birimine göre cüzdanları bul.
            fromWallet = walletService.getWalletByUserEmailAndCurrency(request.getFromCustomerId(), fromCurrency);
            toWallet = walletService.getWalletByUserEmailAndCurrency(request.getToCustomerId(), toCurrency);

            ledgerService.updateBalance(fromWallet.getId(), fromCurrency, fromAmount.negate());

            ledgerService.updateBalance(toWallet.getId(), toCurrency, convertedAmount);

            return handleSuccessfulTransfer(request, fromWallet, toWallet, convertedAmount, rate);

        } catch (Exception e) {
            log.error("Transfer hatası (Rollback tetiklendi): {}", e.getMessage(), e);
            handleFailedTransaction(request, fromWallet, toWallet, e, convertedAmount, rate);
            return null;
        }
    }

    @Transactional
    public Transaction executeFx(FxRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        BigDecimal rate = fxService.getRate(request.getFromCurrency(), request.getToCurrency());
        BigDecimal convertedAmount = fxService.convert(request.getAmount(), request.getFromCurrency(), request.getToCurrency());

        Wallet wallet = null;
        try {
            // Düzeltme: E-posta ve para birimine göre cüzdanı bul.
            wallet = walletService.getWalletByUserEmailAndCurrency(request.getCustomerId(), request.getFromCurrency());

            ledgerService.updateBalance(wallet.getId(), request.getFromCurrency(), request.getAmount().negate());

            ledgerService.updateBalance(wallet.getId(), request.getToCurrency(), convertedAmount);

            return handleSuccessfulFx(request, wallet, convertedAmount, rate);

        } catch (Exception e) {
            log.error("FX Trade hatası (Rollback tetiklendi): {}", e.getMessage(), e);
            handleFailedTransaction(request, wallet, e, convertedAmount, rate);
            return null;
        }
    }

    public Page<Transaction> getTransactionsForUser(String customerId, Pageable pageable) {
        List<Long> userWalletIds = walletRepository.findAllWalletIdsByCustomerId(customerId);
        if (userWalletIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return transactionRepository.findByFromWalletIdInOrToWalletIdIn(userWalletIds, userWalletIds, pageable);
    }

    private void checkIdempotency(String key) {
        if (transactionRepository.existsByIdempotencyKey(key)) {
            throw new IdempotencyException("Bu işlem daha önce gerçekleştirildi: " + key);
        }
    }

    private Transaction createTransactionEntry(String key, Long fromId, Long toId, BigDecimal originAmount, String originCurrency, BigDecimal convertedAmount, String targetCurrency, BigDecimal rate, String desc, TransactionType type, TransactionStatus status) {
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

    private void sendNotification(String customerId, String message, String notificationType) {
        try {
            notificationClient.sendNotification(new NotificationRequest(customerId, message, notificationType));
        } catch (Exception e) {
            log.error("Anlık bildirim gönderilemedi (Ana işlem etkilenmedi): Customer {}, Error: {}", customerId, e.getMessage());
        }
    }

    private void rethrowSpecificExceptions(Exception e, String defaultMessage) {
        if (e instanceof InsufficientFundsException || e instanceof EntityNotFoundException || e instanceof IdempotencyException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(defaultMessage, e);
    }

    private Transaction handleSuccessfulDeposit(DepositRequest request, Wallet wallet) {
        Transaction transaction = createTransactionEntry(request.getIdempotencyKey(), null, wallet.getId(), request.getAmount(), request.getCurrency(), request.getAmount(), request.getCurrency(), BigDecimal.ONE, request.getDescription(), TransactionType.DEPOSIT, TransactionStatus.COMPLETED);
        sendNotification(wallet.getUser().getCustomerId(), String.format("%.2f %s cüzdanınıza yüklendi.", request.getAmount(), request.getCurrency()), "DEPOSIT_COMPLETED");
        eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));
        return transaction;
    }

    private Transaction handleSuccessfulWithdrawal(WithdrawRequest request, Wallet wallet) {
        Transaction transaction = createTransactionEntry(request.getIdempotencyKey(), wallet.getId(), null, request.getAmount(), request.getCurrency(), request.getAmount(), request.getCurrency(), BigDecimal.ONE, request.getDescription(), TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED);
        sendNotification(wallet.getUser().getCustomerId(), String.format("%.2f %s cüzdanınızdan çekildi.", request.getAmount(), request.getCurrency()), "WITHDRAWAL_COMPLETED");
        eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));
        return transaction;
    }

    private Transaction handleSuccessfulTransfer(TransferRequest request, Wallet fromWallet, Wallet toWallet, BigDecimal convertedAmount, BigDecimal rate) {
        Transaction transaction = createTransactionEntry(request.getIdempotencyKey(), fromWallet.getId(), toWallet.getId(), request.getAmount(), request.getCurrency(), convertedAmount, request.getTargetCurrency(), rate, request.getDescription(), TransactionType.TRANSFER, TransactionStatus.COMPLETED);
        sendNotification(fromWallet.getUser().getCustomerId(), String.format("%.2f %s gönderdiniz.", request.getAmount(), request.getCurrency()), "TRANSFER_SENT");
        sendNotification(toWallet.getUser().getCustomerId(), String.format("%.2f %s aldınız.", convertedAmount, request.getTargetCurrency()), "TRANSFER_RECEIVED");
        eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));
        return transaction;
    }

    private Transaction handleSuccessfulFx(FxRequest request, Wallet wallet, BigDecimal convertedAmount, BigDecimal rate) {
        Transaction transaction = createTransactionEntry(request.getIdempotencyKey(), wallet.getId(), wallet.getId(), request.getAmount(), request.getFromCurrency(), convertedAmount, request.getToCurrency(), rate, "Döviz Alım/Satım", TransactionType.FX_TRADE, TransactionStatus.COMPLETED);
        sendNotification(wallet.getUser().getCustomerId(), String.format("%.2f %s sattınız, %.2f %s aldınız.", request.getAmount(), request.getFromCurrency(), convertedAmount, request.getToCurrency()), "FX_TRADE_COMPLETED");
        eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));
        return transaction;
    }

    private void handleFailedTransaction(DepositRequest request, Wallet wallet, Exception e) {
        createTransactionEntry(request.getIdempotencyKey(), null, wallet != null ? wallet.getId() : null, request.getAmount(), request.getCurrency(), request.getAmount(), request.getCurrency(), BigDecimal.ONE, "BAŞARISIZ: " + e.getMessage(), TransactionType.DEPOSIT, TransactionStatus.FAILED);
        rethrowSpecificExceptions(e, "Deposit işlemi sırasında hata oluştu.");
    }

    private void handleFailedTransaction(WithdrawRequest request, Wallet wallet, Exception e) {
        String reason = (e instanceof InsufficientFundsException) ? "Yetersiz Bakiye" : e.getMessage();
        createTransactionEntry(request.getIdempotencyKey(), wallet != null ? wallet.getId() : null, null, request.getAmount(), request.getCurrency(), request.getAmount(), request.getCurrency(), BigDecimal.ONE, "BAŞARISIZ: " + reason, TransactionType.WITHDRAWAL, TransactionStatus.FAILED);
        rethrowSpecificExceptions(e, "Withdraw işlemi sırasında hata oluştu.");
    }

    private void handleFailedTransaction(TransferRequest request, Wallet fromWallet, Wallet toWallet, Exception e, BigDecimal convertedAmount, BigDecimal rate) {
        createTransactionEntry(request.getIdempotencyKey(), fromWallet != null ? fromWallet.getId() : null, toWallet != null ? toWallet.getId() : null, request.getAmount(), request.getCurrency(), convertedAmount, request.getTargetCurrency(), rate, "BAŞARISIZ: " + e.getMessage(), TransactionType.TRANSFER, TransactionStatus.FAILED);
        rethrowSpecificExceptions(e, "Transfer sırasında beklenmeyen hata");
    }

    private void handleFailedTransaction(FxRequest request, Wallet wallet, Exception e, BigDecimal convertedAmount, BigDecimal rate) {
        createTransactionEntry(request.getIdempotencyKey(), wallet != null ? wallet.getId() : null, wallet != null ? wallet.getId() : null, request.getAmount(), request.getFromCurrency(), convertedAmount, request.getToCurrency(), rate, "BAŞARISIZ: " + e.getMessage(), TransactionType.FX_TRADE, TransactionStatus.FAILED);
        rethrowSpecificExceptions(e, "FX işlemi sırasında beklenmeyen hata");
    }

    @Transactional
    public Transaction externalTransfer(ExternalTransferRequest request) {
        checkIdempotency(request.getIdempotencyKey());

        // 1. Cüzdanı ve Bakiyeyi Kontrol Et
        Wallet fromWallet = walletService.getWalletByUserEmailAndCurrency(
                request.getFromCustomerId(),
                request.getCurrency()
        );

        // 2. Komisyon Hesabı (Tutar * 0.05)
        BigDecimal fee = request.getAmount().multiply(SWIFT_FEE_RATE);
        BigDecimal totalDeduction = request.getAmount().add(fee);

        log.info("Dış Transfer: Tutar={}, Komisyon={}, Toplam Düşülecek={}", request.getAmount(), fee, totalDeduction);

        try {
            // 3. Parayı (Tutar + Komisyon) Müşteriden Düş
            // LedgerService, bakiye yetersizse hata fırlatacak ve işlem duracaktır.
            ledgerService.updateBalance(fromWallet.getId(), request.getCurrency(), totalDeduction.negate());

            // 4. SWIFT Servisini Çağır (Bu işlem 2-4 saniye sürebilir)
            boolean isSuccess = mockSwiftService.processTransfer(
                    request.getToIban(),
                    request.getSwiftCode(),
                    request.getReceiverName(),
                    request.getAmount().doubleValue()
            );

            // 5. SWIFT Başarısız Olduysa Hata Fırlat (Otomatik Rollback yapar ve parayı iade eder)
            if (!isSuccess) {
                throw new RuntimeException("Karşı banka transferi reddetti.");
            }

            // 6. Başarılıysa Kayıt At
            return handleSuccessfulExternalTransfer(request, fromWallet, fee);

        } catch (Exception e) {
            log.error("Dış transfer hatası: {}", e.getMessage());

            // Hata mesajını güvenli boyuta getir (Maksimum 250 karakter alalım ki ön ek ile taşmasın)
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Bilinmeyen Hata";
            if (errorMessage.length() > 250) {
                errorMessage = errorMessage.substring(0, 250) + "...";
            }

            // Veritabanına "BAŞARISIZ" kaydı at
            createTransactionEntry(
                    request.getIdempotencyKey(),
                    fromWallet.getId(),
                    null,
                    request.getAmount(),
                    request.getCurrency(),
                    request.getAmount(),
                    request.getCurrency(),
                    BigDecimal.ONE,
                    "SWIFT BAŞARISIZ: " + errorMessage, // Kırpılmış mesajı kullanıyoruz
                    TransactionType.EXTERNAL_TRANSFER,
                    TransactionStatus.FAILED
            );
            throw new RuntimeException("Dış transfer gerçekleştirilemedi: " + errorMessage);
        }
    }

    // Yardımcı Metot: Başarılı dış transferi kaydet ve bildirim gönder
    private Transaction handleSuccessfulExternalTransfer(ExternalTransferRequest request, Wallet wallet, BigDecimal fee) {
        String description = String.format("SWIFT Transferi (Komisyon: %s %s)", fee, request.getCurrency());

        Transaction transaction = createTransactionEntry(
                request.getIdempotencyKey(),
                wallet.getId(),
                null, // Alıcı cüzdan ID yok (Dışarı gitti)
                request.getAmount(),
                request.getCurrency(),
                request.getAmount(),
                request.getCurrency(),
                BigDecimal.ONE,
                description,
                TransactionType.EXTERNAL_TRANSFER,
                TransactionStatus.COMPLETED
        );

        sendNotification(
                wallet.getUser().getCustomerId(),
                String.format("SWIFT işleminiz onaylandı. Gönderilen: %s %s, Kesilen Komisyon: %s %s",
                        request.getAmount(), request.getCurrency(), fee, request.getCurrency()),
                "SWIFT_SENT"
        );

        // Event fırlat (Email servisi dinliyorsa mail atar)
        eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));

        return transaction;
    }
}
