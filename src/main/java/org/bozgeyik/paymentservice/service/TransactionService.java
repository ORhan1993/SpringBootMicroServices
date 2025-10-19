package org.bozgeyik.paymentservice.service;

import org.bozgeyik.paymentservice.client.NotificationClient;
import org.bozgeyik.paymentservice.dto.NotificationRequest;
import org.bozgeyik.paymentservice.exception.CustomAccountNotFoundException;
import org.bozgeyik.paymentservice.exception.InsufficientFundsException;
import org.bozgeyik.paymentservice.model.Account; // Account nesnesine erişim için
import org.bozgeyik.paymentservice.model.Transaction;
import org.bozgeyik.paymentservice.model.TransactionStatus;
import org.bozgeyik.paymentservice.model.TransactionType;
import org.bozgeyik.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Loglama için
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Lombok ile SLF4J logger ekleyelim
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final NotificationClient notificationClient; // NotificationClient enjekte edildi

    // Sabitler
    private static final String DEFAULT_CURRENCY = "TRY"; // Varsayılan para birimi
    private static final String DEPOSIT_DESCRIPTION_PREFIX = "Para Yatırma - ";
    private static final String WITHDRAWAL_DESCRIPTION_PREFIX = "Para Çekme - ";
    private static final String TRANSFER_DESCRIPTION_PREFIX = "Para Transferi - ";


    // buildTransaction metodu private ve yardımcı bir metot olarak kalabilir.
    private Transaction buildTransaction(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String currency,
                                         String description, TransactionType type, TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccountNumber);
        transaction.setToAccount(toAccountNumber);
        transaction.setAmount(amount);
        transaction.setCurrency(currency != null ? currency : DEFAULT_CURRENCY); // Para birimi null ise varsayılanı kullan
        transaction.setDescription(description);
        transaction.setTransactionType(type);
        transaction.setStatus(status);
        transaction.setTransactionDate(LocalDateTime.now());
        return transaction;
    }

    /**
     * Bir hesaptan başka bir hesaba para transferi gerçekleştirir.
     * Başarılı transfer sonrası bildirim gönderir.
     *
     * @param fromAccountNumber Kaynak hesap numarası
     * @param toAccountNumber Hedef hesap numarası
     * @param amount Transfer edilecek miktar
     * @param description İşlem açıklaması (isteğe bağlı)
     * @return Oluşturulan işlem kaydı
     * @throws CustomAccountNotFoundException Kaynak veya hedef hesap bulunamazsa
     * @throws InsufficientFundsException Kaynak hesapta yeterli bakiye yoksa
     */
    @Transactional
    public Transaction transferMoney(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String description)
            throws CustomAccountNotFoundException, InsufficientFundsException {

        // 1. Hesapların varlığını ve bakiyeyi kontrol et (AccountService bu kontrolleri yapmalı)
        // AccountService.withdraw ve deposit metotları, başarısızlık durumunda exception fırlatmalıdır.
        Account sourceAccount = accountService.withdraw(fromAccountNumber, amount); // Para çekme işlemi ve kaynak hesabı al
        Account targetAccount = accountService.deposit(toAccountNumber, amount);   // Para yatırma işlemi ve hedef hesabı al

        // 2. İşlem kaydını oluştur
        String finalDescription = (description == null || description.trim().isEmpty())
                ? TRANSFER_DESCRIPTION_PREFIX + fromAccountNumber + " -> " + toAccountNumber
                : description;

        Transaction transaction = buildTransaction(
                fromAccountNumber,
                toAccountNumber,
                amount,
                sourceAccount.getCurrency(), // Kaynak hesabın para birimini kullan
                finalDescription,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 3. Bildirimleri Gönder (İşlem başarılı olduktan sonra)
        // Kaynak hesap sahibine bildirim
        sendNotification(
                sourceAccount.getCustomerId(), // Account nesnesinden customerId alınmalı
                String.format("%s numaralı hesaba %.2f %s tutarında para transferi yapıldı. Yeni bakiyeniz: %.2f %s",
                        toAccountNumber, amount, sourceAccount.getCurrency(), sourceAccount.getBalance(), sourceAccount.getCurrency()),
                "TRANSFER_SENT"
        );

        // Hedef hesap sahibine bildirim
        sendNotification(
                targetAccount.getCustomerId(), // Account nesnesinden customerId alınmalı
                String.format("%s numaralı hesaptan %.2f %s tutarında para transferi alındı. Yeni bakiyeniz: %.2f %s",
                        fromAccountNumber, amount, targetAccount.getCurrency(), targetAccount.getBalance(), targetAccount.getCurrency()),
                "TRANSFER_RECEIVED"
        );

        return savedTransaction;
    }

    /**
     * Belirtilen hesaba para yatırır.
     *
     * @param accountNumber Para yatırılacak hesap numarası
     * @param amount Yatırılacak miktar
     * @param currency Para birimi (null ise varsayılan kullanılır)
     * @param description İşlem açıklaması (isteğe bağlı)
     * @return Oluşturulan işlem kaydı
     * @throws CustomAccountNotFoundException Hesap bulunamazsa
     */
    @Transactional
    public Transaction deposit(String accountNumber, BigDecimal amount, String currency, String description) throws CustomAccountNotFoundException {
        Account depositedAccount = accountService.deposit(accountNumber, amount); // Bu metot hesap yoksa exception fırlatmalı

        String finalDescription = (description == null || description.trim().isEmpty())
                ? DEPOSIT_DESCRIPTION_PREFIX + accountNumber
                : description;

        Transaction transaction = buildTransaction(
                null,
                accountNumber,
                amount,
                currency != null ? currency : depositedAccount.getCurrency(), // Hesabın para birimini veya varsayılanı kullan
                finalDescription,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Para yatırma bildirimi
        sendNotification(
                depositedAccount.getCustomerId(),
                String.format("%.2f %s tutarında para yatırıldı. Yeni bakiyeniz: %.2f %s",
                        amount, depositedAccount.getCurrency(), depositedAccount.getBalance(), depositedAccount.getCurrency()),
                "DEPOSIT_COMPLETED"
        );

        return savedTransaction;
    }

    /**
     * Belirtilen hesaptan para çeker.
     *
     * @param accountNumber Para çekilecek hesap numarası
     * @param amount Çekilecek miktar
     * @param currency Para birimi (null ise varsayılan kullanılır)
     * @param description İşlem açıklaması (isteğe bağlı)
     * @return Oluşturulan işlem kaydı
     * @throws CustomAccountNotFoundException Hesap bulunamazsa
     * @throws InsufficientFundsException Hesapta yeterli bakiye yoksa
     */
    @Transactional
    public Transaction withdraw(String accountNumber, BigDecimal amount, String currency, String description)
            throws CustomAccountNotFoundException, InsufficientFundsException {
        Account withdrawnAccount = accountService.withdraw(accountNumber, amount); // Bu metot hesap yoksa veya bakiye yetersizse exception fırlatmalı

        String finalDescription = (description == null || description.trim().isEmpty())
                ? WITHDRAWAL_DESCRIPTION_PREFIX + accountNumber
                : description;

        Transaction transaction = buildTransaction(
                accountNumber,
                null,
                amount,
                currency != null ? currency : withdrawnAccount.getCurrency(), // Hesabın para birimini veya varsayılanı kullan
                finalDescription,
                TransactionType.WITHDRAWAL,
                TransactionStatus.COMPLETED
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Para çekme bildirimi
        sendNotification(
                withdrawnAccount.getCustomerId(),
                String.format("%.2f %s tutarında para çekildi. Yeni bakiyeniz: %.2f %s",
                        amount, withdrawnAccount.getCurrency(), withdrawnAccount.getBalance(), withdrawnAccount.getCurrency()),
                "WITHDRAWAL_COMPLETED"
        );

        return savedTransaction;
    }


    /**
     * Bildirim göndermek için yardımcı metot.
     * @param customerId Müşteri ID'si
     * @param message Bildirim mesajı
     * @param notificationType Bildirim tipi
     */
    private void sendNotification(String customerId, String message, String notificationType) {
        if (customerId == null) {
            log.warn("Customer ID is null, notification cannot be sent for type: {}", notificationType);
            return;
        }
        try {
            log.info("Sending notification to customer: {}, type: {}, message: {}", customerId, notificationType, message);
            notificationClient.sendNotification(new NotificationRequest(
                    customerId,
                    message,
                    notificationType
            ));
            log.info("Notification sent successfully to customer: {}, type: {}", customerId, notificationType);
        } catch (Exception e) {
            // Bildirim hatası ana işlemi etkilememeli, sadece loglanmalı.
            // Daha gelişmiş senaryolarda bu hatalar bir kuyruğa alınıp tekrar denenebilir.
            log.error("Notification could not be sent for customer: {}, type: {}. Error: {}", customerId, notificationType, e.getMessage(), e);
        }
    }

    /**
     * Belirli bir hesap numarasına ait tüm işlemleri getirir.
     *
     * @param accountNumber İşlemleri getirilecek hesap numarası.
     * @return Hesap numarasına ait işlemlerin listesi.
     * @throws CustomAccountNotFoundException Eğer belirtilen hesap numarası bulunamazsa.
     */
    public List<Transaction> getTransactions(String accountNumber) throws CustomAccountNotFoundException {
        // AccountService'in, hesap bulunamazsa CustomAccountNotFoundException fırlatmasını bekliyoruz.
        // Bu kontrol AccountService içinde yapılmalı ve getAccountByAccountNumber gibi bir metot
        // hesap bulunamazsa exception fırlatmalı.
        accountService.getAccountByAccountNumber(accountNumber); // Bu satır hesap varlığını kontrol eder, yoksa exception fırlar.

        return transactionRepository.findByFromAccountOrToAccountOrderByTransactionDateDesc(accountNumber, accountNumber);
    }
}