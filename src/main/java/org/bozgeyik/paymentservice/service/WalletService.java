package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import org.bozgeyik.paymentservice.dto.CreateWalletRequest;
import org.bozgeyik.paymentservice.model.AccountStatus;
import org.bozgeyik.paymentservice.model.User;
import org.bozgeyik.paymentservice.model.Wallet;
import org.bozgeyik.paymentservice.model.WalletBalance;
import org.bozgeyik.paymentservice.repository.UserRepository;
import org.bozgeyik.paymentservice.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Wallet createWallet(CreateWalletRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Cüzdan oluşturmak için geçerli bir kullanıcı bulunamadı: " + request.getEmail()));

        boolean walletExists = walletRepository.existsByUser_IdAndBalances_Currency(user.getId(), request.getDefaultCurrency());
        if (walletExists) {
            throw new IllegalStateException("Bu kullanıcı için " + request.getDefaultCurrency() + " para biriminde zaten bir cüzdan mevcut.");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setStatus(AccountStatus.ACTIVE);

        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) >= 0) {
            WalletBalance initialBalance = new WalletBalance();
            initialBalance.setWallet(wallet);
            initialBalance.setCurrency(request.getDefaultCurrency());
            initialBalance.setBalance(request.getInitialBalance());
            wallet.getBalances().add(initialBalance);
        }

        return walletRepository.save(wallet);
    }

    public Wallet getWalletById(Long walletId) {
        return walletRepository.findWalletWithUserById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Cüzdan bulunamadı: ID " + walletId));
    }

    public Wallet getWalletByUserEmailAndCurrency(String email, String currency) {
        return walletRepository.findByUserEmailAndCurrency(email, currency)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Kullanıcı için cüzdan bulunamadı: " + email + ", Para Birimi: " + currency));
    }

    // YENİ EKLENEN: Kullanıcının tüm cüzdanlarını getir
    public List<Wallet> getWalletsByUserEmail(String email) {
        return walletRepository.findAllByUserEmail(email);
    }

    @Transactional
    public void closeWallet(Long walletId) {
        Wallet wallet = getWalletById(walletId);

        boolean hasBalance = wallet.getBalances().stream()
                .anyMatch(b -> b.getBalance().compareTo(BigDecimal.ZERO) > 0);

        if (hasBalance) {
            throw new IllegalStateException("Cüzdan kapatılamaz. Cüzdanda hala bakiye bulunmaktadır.");
        }

        wallet.setStatus(AccountStatus.CLOSED);
        walletRepository.save(wallet);
    }
}