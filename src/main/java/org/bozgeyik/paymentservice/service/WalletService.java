package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.dto.CreateWalletRequest;
import org.bozgeyik.paymentservice.model.AccountStatus;
import org.bozgeyik.paymentservice.model.Wallet;
import org.bozgeyik.paymentservice.model.WalletBalance;
import org.bozgeyik.paymentservice.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet createWallet(CreateWalletRequest request) {
        if (walletRepository.existsByCustomerId(request.getCustomerId())) {
            throw new IllegalArgumentException("Bu müşteri ID'si ile cüzdan zaten mevcut: " + request.getCustomerId());
        }

        Wallet wallet = new Wallet();
        wallet.setCustomerId(request.getCustomerId());
        wallet.setOwnerName(request.getOwnerName());
        wallet.setEmail(request.getEmail()); // <-- YENİ EKLENDİ
        wallet.setStatus(AccountStatus.ACTIVE);

        // İlk bakiye satırını oluştur
        if (request.getInitialBalance() != null && request.getInitialBalance().compareTo(BigDecimal.ZERO) >= 0) {
            WalletBalance initialBalance = new WalletBalance();
            initialBalance.setWallet(wallet);
            initialBalance.setCurrency(request.getDefaultCurrency());
            initialBalance.setBalance(request.getInitialBalance());
            wallet.getBalances().add(initialBalance);
        }

        return walletRepository.save(wallet);
    }

    public Wallet getWalletByCustomerId(String customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Müşteri cüzdanı bulunamadı: " + customerId));
    }

    public Wallet getWalletById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Cüzdan bulunamadı: " + walletId));
    }

    @Transactional
    public void closeWallet(String customerId) {
        Wallet wallet = getWalletByCustomerId(customerId);

        // İş Kuralı: Bakiyesi olan cüzdan kapatılamaz
        boolean hasBalance = wallet.getBalances().stream()
                .anyMatch(b -> b.getBalance().compareTo(BigDecimal.ZERO) > 0);

        if (hasBalance) {
            throw new IllegalStateException("Cüzdan kapatılamaz. Aktif bakiye mevcut.");
        }

        wallet.setStatus(AccountStatus.CLOSED);
        walletRepository.save(wallet);
    }
}