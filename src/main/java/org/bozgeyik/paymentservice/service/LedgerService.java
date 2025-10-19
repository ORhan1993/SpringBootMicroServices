package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bozgeyik.paymentservice.exception.InsufficientFundsException;
import org.bozgeyik.paymentservice.model.Wallet;
import org.bozgeyik.paymentservice.model.WalletBalance;
import org.bozgeyik.paymentservice.repository.WalletBalanceRepository;
import org.bozgeyik.paymentservice.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository balanceRepository;

    /**
     * Bir cüzdan bakiyesini ATOMİK olarak günceller.
     * Bu metot, çağıran servisin (Orchestrator) Transaction'ına katılır (Propagation.REQUIRED).
     * Bakiye satırını PESSIMISTIC_WRITE ile kilitler.
     *
     * @param walletId Cüzdan ID'si
     * @param currency Para birimi
     * @param amountDelta Değişim (+giriş, -çıkış)
     * @throws InsufficientFundsException Bakiye yetersizse
     * @throws EntityNotFoundException Cüzdan bulunamazsa
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public WalletBalance updateBalance(Long walletId, String currency, BigDecimal amountDelta)
            throws InsufficientFundsException, EntityNotFoundException {

        // 1. Cüzdanı bul
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Cüzdan bulunamadı: " + walletId));

        // 2. KİLİTLE: Bakiye satırını 'FOR UPDATE' ile çek
        log.info("Kilit isteniyor: Cüzdan {} - Para Birimi {}", walletId, currency);
        WalletBalance balance = balanceRepository
                .findByWalletAndCurrencyForUpdate(wallet, currency)
                .orElseGet(() -> {
                    // Bu para biriminde ilk kez bakiye oluşturuluyor
                    log.info("Yeni bakiye satırı oluşturuluyor: Cüzdan {} - {}", walletId, currency);
                    WalletBalance newBalance = new WalletBalance();
                    newBalance.setWallet(wallet);
                    newBalance.setCurrency(currency);
                    newBalance.setBalance(BigDecimal.ZERO);
                    return newBalance;
                });
        log.info("Kilit alındı: Cüzdan {} - Para Birimi {}", walletId, currency);

        // 3. Yeni bakiyeyi hesapla
        BigDecimal newBalance = balance.getBalance().add(amountDelta);

        // 4. Bakiye Kontrolü (Para çıkışıysa)
        if (amountDelta.compareTo(BigDecimal.ZERO) < 0 && newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Yetersiz Bakiye: Cüzdan {}, İstenen {}, Mevcut {}", walletId, amountDelta, balance.getBalance());
            throw new InsufficientFundsException(
                    String.format("Yetersiz Bakiye: %s cüzdanında %s birimi için bakiye yok.", walletId, currency)
            );
        }

        // 5. Güncelle ve kaydet
        balance.setBalance(newBalance);
        WalletBalance savedBalance = balanceRepository.save(balance);
        log.info("Kilit serbest bırakıldı ve bakiye güncellendi: Cüzdan {} - Yeni Bakiye {}", walletId, newBalance);

        return savedBalance;
    }
}