package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bozgeyik.paymentservice.event.TransactionCompletedEvent;
import org.bozgeyik.paymentservice.model.Transaction;
import org.bozgeyik.paymentservice.model.TransactionType;
import org.bozgeyik.paymentservice.model.Wallet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine; // <-- YENİ
import org.thymeleaf.context.Context; // <-- YENİ

import java.time.LocalDateTime; // <-- YENİ (Tarih için)
import java.time.format.DateTimeFormatter; // <-- YENİ (Tarih için)
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailService emailService;
    private final WalletService walletService;
    private final TemplateEngine templateEngine; // <-- YENİ: Thymeleaf motorunu inject et

    // Tarih formatı için sabit
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handleTransactionCompletion(TransactionCompletedEvent event) {
        log.info("İşlem tamamlanma olayı alındı, e-posta hazırlanıyor...");
        Transaction tx = event.getTransaction();

        // if-else if zinciri yerine switch ifadesi daha okunabilir ve modern bir yaklaşımdır.
        switch (tx.getTransactionType()) {
            case TRANSFER -> sendTransferEmails(tx);
            case DEPOSIT -> sendDepositEmail(tx);
            case WITHDRAWAL -> sendWithdrawalEmail(tx);
            case FX_TRADE -> sendFxTradeEmail(tx);
            default -> log.warn("Desteklenmeyen işlem türü için e-posta bildirimi atlandı: {}", tx.getTransactionType());
        }
    }

    private void sendTransferEmails(Transaction tx) {
        // Cüzdan bilgilerini (ve ilişkili kullanıcı bilgilerini) al
        Optional<Wallet> fromWalletOpt = findWalletById(tx.getFromWalletId());
        Optional<Wallet> toWalletOpt = findWalletById(tx.getToWalletId());

        // Gönderici ve alıcı cüzdanları mevcutsa e-postaları gönder.
        // DÜZELTME: ifPresent lambda'ları içindeki değişkenler doğru kullanıldı.
        // Önceden .get() ile yapılan gereksiz ve hatalı çağrılar kaldırıldı.
        fromWalletOpt.ifPresent(fromWallet -> toWalletOpt.ifPresent(toWallet -> {
            // 1. Göndericiye E-posta
            Context senderContext = createEmailContext(tx, fromWallet, "Para Transferi", "Başka Bankaya Transfer", "Hesabınızdan para transferi gerçekleştirildi.");
            senderContext.setVariable("gonderenAdi", fromWallet.getUserName());
            // DÜZELTME: IBAN ve Banka Adı bilgileri Wallet nesnesinden alınmalı.
            senderContext.setVariable("gonderenHesap", fromWallet.getIban()); // Varsayım: Wallet'ta iban alanı var.
            senderContext.setVariable("aliciAdi", toWallet.getUserName());
            senderContext.setVariable("aliciIban", toWallet.getIban());
            senderContext.setVariable("aliciBanka", toWallet.getBankName());
            senderContext.setVariable("tutar", String.format("%.2f %s", tx.getOriginalAmount(), tx.getOriginalCurrency()));

            processAndSendEmail(fromWallet.getEmail(), "Para Transferi Gerçekleştirildi", "email/transfer-bildirimi", senderContext);

            // 2. Alıcıya E-posta
            Context receiverContext = createEmailContext(tx, toWallet, "Para Transferi", "Hesaba Gelen Transfer", "Hesabınıza para transferi geldi.");
            receiverContext.setVariable("gonderenAdi", fromWallet.getUserName());
            receiverContext.setVariable("gonderenHesap", fromWallet.getIban());
            receiverContext.setVariable("aliciAdi", toWallet.getUserName());
            receiverContext.setVariable("aliciIban", toWallet.getIban()); // Varsayım: Wallet'ta iban alanı var.
            receiverContext.setVariable("aliciBanka", toWallet.getBankName()); // Varsayım: Wallet'ta bankName alanı var.
            receiverContext.setVariable("tutar", String.format("%.2f %s", tx.getConvertedAmount(), tx.getTargetCurrency()));

            processAndSendEmail(
                    toWallet.getEmail(),
                    "Hesabınıza Para Geldi",
                    "email/transfer-bildirimi",
                    receiverContext);
        }));
    }

    private void sendDepositEmail(Transaction tx) {
        findWalletById(tx.getToWalletId()).ifPresent(wallet -> {
            Context context = createEmailContext(tx, wallet, "Para Yatırma", "Hesabınıza Para Yüklendi", "Hesabınıza başarıyla para yatırılmıştır.");
            context.setVariable("tutar", String.format("%.2f %s", tx.getConvertedAmount(), tx.getTargetCurrency()));

            processAndSendEmail(wallet.getEmail(), "Hesabınıza Para Yüklendi", "email/islem-bildirimi", context);
        });
    }

    private void sendWithdrawalEmail(Transaction tx) {
        findWalletById(tx.getFromWalletId()).ifPresent(wallet -> {
            Context context = createEmailContext(tx, wallet, "Para Çekme", "Hesabınızdan Para Çekildi", "Hesabınızdan başarıyla para çekilmiştir.");
            context.setVariable("tutar", String.format("%.2f %s", tx.getOriginalAmount(), tx.getOriginalCurrency()));

            processAndSendEmail(wallet.getEmail(), "Hesabınızdan Para Çekildi", "email/islem-bildirimi", context);
        });
    }

    private void sendFxTradeEmail(Transaction tx) {
        findWalletById(tx.getFromWalletId()).ifPresent(wallet -> {
            Context context = createEmailContext(tx, wallet, "Döviz Alım/Satım", "Döviz İşlemi Tamamlandı", "Döviz alım/satım işleminiz başarıyla gerçekleştirildi.");
            context.setVariable("satilanTutar", String.format("%.2f %s", tx.getOriginalAmount(), tx.getOriginalCurrency()));
            context.setVariable("alinanTutar", String.format("%.2f %s", tx.getConvertedAmount(), tx.getTargetCurrency()));
            context.setVariable("kur", tx.getExchangeRateUsed());

            processAndSendEmail(wallet.getEmail(), "Döviz Alım/Satım İşlemi Tamamlandı", "email/fx-trade-bildirimi", context);
        });
    }

    /**
     * YENİ: Tekrarlanan e-posta gönderme ve şablon işleme mantığı
     */
    private void processAndSendEmail(String to, String subject, String templateName, Context context) {
        try {
            // Thymeleaf şablonunu HTML string'ine dönüştür
            String htmlBody = templateEngine.process(templateName, context);
            emailService.sendHtmlEmail(to, subject, htmlBody);
        } catch (Exception e) {
            log.error("E-posta şablonu işlenirken veya gönderilirken hata oluştu: {}", e.getMessage(), e);
        }
    }

    /**
     * YENİ: E-posta context'ini oluşturan ve ortak değişkenleri ayarlayan yardımcı metot.
     */
    private Context createEmailContext(Transaction tx, Wallet wallet, String mailBaslik, String anaBaslik, String aciklamaMetni) {
        Context context = new Context();
        context.setVariable("mailBaslik", mailBaslik);
        context.setVariable("anaBaslik", anaBaslik);
        context.setVariable("aciklamaMetni", aciklamaMetni);
        context.setVariable("musteriAdi", wallet.getUserName());
        context.setVariable("islemId", tx.getIdempotencyKey());
        context.setVariable("aciklama", tx.getDescription());

        // HATA DÜZELTMESİ: Transaction entity'sinde 'createdAt' değil, 'transactionDate' alanı var.
        // @CreationTimestamp sayesinde bu alan null olmayacağı için kontrol basitleştirildi.
        String transactionDate = tx.getTransactionDate().format(DATE_FORMATTER);
        context.setVariable("islemTarihi", transactionDate);

        return context;
    }

    /**
     * GÜNCELLENDİ: E-posta adresi yerine tüm Wallet nesnesini döndürür.
     * Bu, şablonda (isim, iban vb.) daha fazla bilgi kullanmamızı sağlar.
     */
    private Optional<Wallet> findWalletById(Long walletId) {
        if (walletId == null) {
            return Optional.empty();
        }
        try {
            // getWalletById'nin cüzdanla birlikte kullanıcı bilgilerini
            // (veya ihtiyaç duyulan diğer bilgileri) getirdiğini varsayıyoruz.
            Wallet wallet = walletService.getWalletById(walletId);
            return Optional.of(wallet);
        } catch (EntityNotFoundException e) { // Daha spesifik bir exception yakalamak daha iyidir.
            log.error("E-posta için cüzdan bilgisi bulunamadı: {}", walletId, e);
            return Optional.empty();
        }
    }
}