package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bozgeyik.paymentservice.config.RabbitMQConfig;
import org.bozgeyik.paymentservice.dto.EmailMessage;
import org.bozgeyik.paymentservice.event.TransactionCompletedEvent;
import org.bozgeyik.paymentservice.model.Transaction;
import org.bozgeyik.paymentservice.model.Wallet;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final WalletService walletService;
    private final TemplateEngine templateEngine;
    private final RabbitTemplate rabbitTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handleTransactionCompletion(TransactionCompletedEvent event) {
        log.info("İşlem tamamlanma olayı alındı, bildirim hazırlanıyor...");
        Transaction tx = event.getTransaction();

        switch (tx.getTransactionType()) {
            case TRANSFER -> sendTransferEmails(tx);
            case DEPOSIT -> sendDepositEmail(tx);
            case WITHDRAWAL -> sendWithdrawalEmail(tx);
            case FX_TRADE -> sendFxTradeEmail(tx);
            case EXTERNAL_TRANSFER -> sendExternalTransferEmail(tx);
            default -> log.warn("Bildirim atlandı (Desteklenmeyen Tür): {}", tx.getTransactionType());
        }
    }

    private void sendExternalTransferEmail(Transaction tx) {
        findWalletById(tx.getFromWalletId()).ifPresent(wallet -> {
            Context context = createEmailContext(tx, wallet, "SWIFT Transferi", "Yurt Dışı Transfer", "SWIFT işleminiz başarıyla alınmıştır.");
            context.setVariable("tutar", String.format("%.2f %s", tx.getOriginalAmount(), tx.getOriginalCurrency()));

            // Kuyruğa gönder
            processAndSendEmail(wallet.getEmail(), "SWIFT İşleminiz Alındı", "email/islem-bildirimi", context);
        });
    }

    private void sendTransferEmails(Transaction tx) {
        Optional<Wallet> fromWalletOpt = findWalletById(tx.getFromWalletId());
        Optional<Wallet> toWalletOpt = findWalletById(tx.getToWalletId());

        fromWalletOpt.ifPresent(fromWallet -> toWalletOpt.ifPresent(toWallet -> {
            // Göndericiye Bildirim
            Context senderContext = createEmailContext(tx, fromWallet, "Para Transferi", "Başka Bankaya Transfer", "Hesabınızdan para transferi gerçekleştirildi.");
            senderContext.setVariable("gonderenAdi", fromWallet.getUserName());
            senderContext.setVariable("gonderenHesap", fromWallet.getIban());
            senderContext.setVariable("aliciAdi", toWallet.getUserName());
            senderContext.setVariable("aliciIban", toWallet.getIban());
            senderContext.setVariable("aliciBanka", toWallet.getBankName());
            senderContext.setVariable("tutar", String.format("%.2f %s", tx.getOriginalAmount(), tx.getOriginalCurrency()));
            processAndSendEmail(fromWallet.getEmail(), "Para Transferi Gerçekleştirildi", "email/transfer-bildirimi", senderContext);

            // Alıcıya Bildirim
            Context receiverContext = createEmailContext(tx, toWallet, "Para Transferi", "Hesaba Gelen Transfer", "Hesabınıza para transferi geldi.");
            receiverContext.setVariable("gonderenAdi", fromWallet.getUserName());
            receiverContext.setVariable("gonderenHesap", fromWallet.getIban());
            receiverContext.setVariable("aliciAdi", toWallet.getUserName());
            receiverContext.setVariable("aliciIban", toWallet.getIban());
            receiverContext.setVariable("aliciBanka", toWallet.getBankName());
            receiverContext.setVariable("tutar", String.format("%.2f %s", tx.getConvertedAmount(), tx.getTargetCurrency()));
            processAndSendEmail(toWallet.getEmail(), "Hesabınıza Para Geldi", "email/transfer-bildirimi", receiverContext);
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

    private void processAndSendEmail(String to, String subject, String templateName, Context context) {
        try {
            String htmlBody = templateEngine.process(templateName, context);
            EmailMessage message = new EmailMessage(to, subject, htmlBody);

            // RabbitMQ kuyruğuna asenkron gönderim
            rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, message);

            log.info("E-posta kuyruğa başarıyla bırakıldı: {}", to);
        } catch (Exception e) {
            log.error("Kuyruğa ekleme hatası: {}", e.getMessage(), e);
        }
    }

    private Context createEmailContext(Transaction tx, Wallet wallet, String mailBaslik, String anaBaslik, String aciklamaMetni) {
        Context context = new Context();
        context.setVariable("mailBaslik", mailBaslik);
        context.setVariable("anaBaslik", anaBaslik);
        context.setVariable("aciklamaMetni", aciklamaMetni);
        context.setVariable("musteriAdi", wallet.getUserName());
        context.setVariable("islemId", tx.getIdempotencyKey());
        context.setVariable("aciklama", tx.getDescription());
        context.setVariable("islemTarihi", tx.getTransactionDate().format(DATE_FORMATTER));
        return context;
    }

    private Optional<Wallet> findWalletById(Long walletId) {
        if (walletId == null) {
            return Optional.empty();
        }
        try {
            Wallet wallet = walletService.getWalletById(walletId);
            return Optional.of(wallet);
        } catch (EntityNotFoundException e) {
            log.error("E-posta için cüzdan bilgisi bulunamadı: {}", walletId, e);
            return Optional.empty();
        }
    }
}