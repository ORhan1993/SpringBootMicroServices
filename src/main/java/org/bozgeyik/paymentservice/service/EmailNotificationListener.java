package org.bozgeyik.paymentservice.service;

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

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailService emailService;
    private final WalletService walletService; // Sadece bu servis yeterli
    // private final UserService userService; // <-- BU SATIRI SİLİN


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handleTransactionCompletion(TransactionCompletedEvent event) {
        log.info("İşlem tamamlanma olayı alındı, e-posta hazırlanıyor...");
        Transaction tx = event.getTransaction();

        try {
            if (tx.getTransactionType() == TransactionType.TRANSFER) {
                sendTransferEmails(tx);
            } else if (tx.getTransactionType() == TransactionType.DEPOSIT) {
                sendDepositEmail(tx);
            } else if (tx.getTransactionType() == TransactionType.WITHDRAWAL) {
                sendWithdrawalEmail(tx);
            } else if (tx.getTransactionType() == TransactionType.FX_TRADE) {
                sendFxTradeEmail(tx);
            }
        } catch (Exception e) {
            log.error("E-posta olayı işlenirken hata oluştu (işlem zaten tamamlandı): {}", e.getMessage(), e);
        }
    }

    // Göndericiye e-posta
    private void sendTransferEmails(Transaction tx) {
        findEmailByWalletId(tx.getFromWalletId()).ifPresent(email -> {
            String subject = "Para Transferi Gönderildi";
            String body = String.format(
                    "Merhaba,<br><br>" +
                            "Hesabınızdan <b>%.2f %s</b> tutarında para transferi gerçekleştirildi.<br>" +
                            "<b>Alıcı Cüzdan ID:</b> %s<br>" +
                            "<b>İşlem ID:</b> %s",
                    tx.getOriginalAmount(), tx.getOriginalCurrency(), tx.getToWalletId(), tx.getIdempotencyKey()
            );
            emailService.sendHtmlEmail(email, subject, body);
        });

        // Alıcıya e-posta
        findEmailByWalletId(tx.getToWalletId()).ifPresent(email -> {
            String subject = "Para Transferi Aldınız";
            String body = String.format(
                    "Merhaba,<br><br>" +
                            "Hesabınıza <b>%.2f %s</b> tutarında para transferi geldi.<br>" +
                            "<b>Gönderen Cüzdan ID:</b> %s<br>" +
                            "<b>İşlem ID:</b> %s",
                    tx.getConvertedAmount(), tx.getTargetCurrency(), tx.getFromWalletId(), tx.getIdempotencyKey()
            );
            emailService.sendHtmlEmail(email, subject, body);
        });
    }

    // Para yatırma e-postası
    private void sendDepositEmail(Transaction tx) {
        findEmailByWalletId(tx.getToWalletId()).ifPresent(email -> {
            String subject = "Hesabınıza Para Yüklendi";
            String body = String.format(
                    "Merhaba,<br><br>" +
                            "Hesabınıza <b>%.2f %s</b> tutarında para yüklendi.<br>" +
                            "<b>İşlem ID:</b> %s",
                    tx.getConvertedAmount(), tx.getTargetCurrency(), tx.getIdempotencyKey()
            );
            emailService.sendHtmlEmail(email, subject, body);
        });
    }

    // Para çekme e-postası
    private void sendWithdrawalEmail(Transaction tx) {
        findEmailByWalletId(tx.getFromWalletId()).ifPresent(email -> {
            String subject = "Hesabınızdan Para Çekildi";
            String body = String.format(
                    "Merhaba,<br><br>" +
                            "Hesabınızdan <b>%.2f %s</b> tutarında para çekildi.<br>" +
                            "<b>İşlem ID:</b> %s",
                    tx.getOriginalAmount(), tx.getOriginalCurrency(), tx.getIdempotencyKey()
            );
            emailService.sendHtmlEmail(email, subject, body);
        });
    }

    // Döviz alım/satım e-postası
    private void sendFxTradeEmail(Transaction tx) {
        findEmailByWalletId(tx.getFromWalletId()).ifPresent(email -> {
            String subject = "Döviz Alım/Satım İşlemi Tamamlandı";
            String body = String.format(
                    "Merhaba,<br><br>" +
                            "<b>%.2f %s</b> satarak <b>%.2f %s</b> aldınız.<br>" +
                            "<b>Kullanılan Kur:</b> %s<br>" +
                            "<b>İşlem ID:</b> %s",
                    tx.getOriginalAmount(), tx.getOriginalCurrency(),
                    tx.getConvertedAmount(), tx.getTargetCurrency(),
                    tx.getExchangeRateUsed(), tx.getIdempotencyKey()
            );
            emailService.sendHtmlEmail(email, subject, body);
        });
    }

    /**
     * GÜNCELLENDİ: Artık UserService'e gerek yok.
     * E-posta adresini doğrudan Wallet nesnesinden okur.
     */
    private Optional<String> findEmailByWalletId(Long walletId) {
        if (walletId == null) {
            return Optional.empty();
        }
        try {
            Wallet wallet = walletService.getWalletById(walletId);
            return Optional.of(wallet.getEmail()); // <-- DOĞRUDAN E-POSTAYI AL
        } catch (Exception e) {
            log.error("E-posta için cüzdan bilgisi bulunamadı: {}", walletId, e);
            return Optional.empty();
        }
    }
}