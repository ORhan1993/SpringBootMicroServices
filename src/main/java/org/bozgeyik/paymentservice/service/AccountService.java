package org.bozgeyik.paymentservice.service;

import org.bozgeyik.paymentservice.exception.CustomAccountNotFoundException; // Kendi özel exception sınıfınız
import org.bozgeyik.paymentservice.exception.InsufficientFundsException; // Kendi özel exception sınıfınız
import org.bozgeyik.paymentservice.model.Account;
import org.bozgeyik.paymentservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Oluşturma ve güncelleme zamanları için
import java.util.Optional; // findByAccountNumber'ın dönüş tipi için

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    /**
     * Yeni bir hesap oluşturur.
     *
     * @param customerId Müşteri ID'si
     * @param accountNumber Hesap Numarası
     * @param initialBalance Başlangıç bakiyesi (isteğe bağlı, varsayılan 0 olabilir)
     * @param currency Para birimi
     * @param ownerName Hesap sahibi adı
     * @return Oluşturulan hesap nesnesi.
     */
    @Transactional
    public Account createAccount(String customerId, String accountNumber, String ownerName, String currency, BigDecimal initialBalance) {
        // Aynı hesap numarasının daha önce oluşturulup oluşturulmadığını kontrol et
        Optional<Account> existingAccount = accountRepository.findByAccountNumber(accountNumber);
        if (existingAccount.isPresent()) {
            throw new IllegalArgumentException("Bu hesap numarası zaten mevcut: " + accountNumber);
        }

        Account account = new Account();
        account.setCustomerId(customerId);
        account.setAccountNumber(accountNumber);
        account.setOwnerName(ownerName); // Hesap sahibi adı eklendi
        account.setCurrency(currency);   // Para birimi eklendi
        account.setBalance(initialBalance != null ? initialBalance : BigDecimal.ZERO); // Başlangıç bakiyesi
        account.setCreatedAt(LocalDateTime.now()); // Oluşturulma zamanı
        account.setUpdatedAt(LocalDateTime.now()); // Güncellenme zamanı (ilk oluşturmada aynı)
        // account.setStatus(AccountStatus.ACTIVE); // Hesap durumu eklenebilir
        return accountRepository.save(account);
    }

    /**
     * Belirtilen hesap numarasını kullanarak hesabı getirir.
     *
     * @param accountNumber Getirilecek hesap numarası.
     * @return Bulunan hesap nesnesi.
     * @throws CustomAccountNotFoundException Hesap bulunamazsa.
     */
    public Account getAccountByAccountNumber(String accountNumber) throws CustomAccountNotFoundException {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomAccountNotFoundException("Hesap bulunamadı: " + accountNumber));
    }

    /**
     * Belirtilen hesap numarasının sistemde var olup olmadığını kontrol eder.
     *
     * @param accountNumber Kontrol edilecek hesap numarası.
     * @return Hesap varsa true, yoksa false.
     */
    public boolean doesAccountExist(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).isPresent();
        // Veya Spring Data JPA'nın `existsBy...` özelliğini kullanabilirsiniz:
        // return accountRepository.existsByAccountNumber(accountNumber);
        // Bunun için AccountRepository'de `boolean existsByAccountNumber(String accountNumber);` tanımlanmalı.
    }

    /**
     * Belirtilen hesaba para yatırır.
     *
     * @param accountNumber Para yatırılacak hesap numarası.
     * @param amount Yatırılacak miktar.
     * @throws CustomAccountNotFoundException Hesap bulunamazsa.
     */
    @Transactional
    public Account deposit(String accountNumber, BigDecimal amount) throws CustomAccountNotFoundException {
        Account account = getAccountByAccountNumber(accountNumber); // Hesap yoksa exception fırlar
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account); // Güncellenmiş hesabı döndür
    }

    /**
     * Belirtilen hesaptan para çeker.
     *
     * @param accountNumber Para çekilecek hesap numarası.
     * @param amount Çekilecek miktar.
     * @throws CustomAccountNotFoundException Hesap bulunamazsa.
     * @throws InsufficientFundsException Hesapta yeterli bakiye yoksa.
     */
    @Transactional
    public Account withdraw(String accountNumber, BigDecimal amount) throws CustomAccountNotFoundException, InsufficientFundsException {
        Account account = getAccountByAccountNumber(accountNumber);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Hesapta yetersiz bakiye: " + accountNumber);
        }
        account.setBalance(account.getBalance().subtract(amount));
        return accountRepository.save(account); // Güncellenmiş hesabı döndür
    }


}