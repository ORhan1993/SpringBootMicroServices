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

/**
 * Muhasebe (Ledger) işlemlerini yöneten servis sınıfı.
 * Bu servis, cüzdan bakiyelerinin atomik ve tutarlı bir şekilde güncellenmesinden sorumludur.
 * Para transferi gibi işlemlerin temelini oluşturur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository balanceRepository;

    /**
     * Bir cüzdanın belirli bir para birimindeki bakiyesini atomik olarak günceller.
     * Bu metot, {@code PESSIMISTIC_WRITE} kilidi kullanarak, aynı bakiye üzerinde eş zamanlı olarak
     * birden fazla işlemin çalışmasını engeller ve veri bütünlüğünü garanti altına alır.
     * Metot, kendisini çağıran servisin (örneğin, PaymentOrchestratorService) mevcut transaction'ına katılır.
     * Eğer çağıran serviste bir transaction yoksa, yeni bir tane başlatılır.
     *
     * @param walletId    Bakiyesi güncellenecek cüzdanın ID'si.
     * @param currency    Güncellenecek bakiyenin para birimi.
     * @param amountDelta Bakiyedeki değişiklik miktarı. Pozitif değer para girişi, negatif değer para çıkışı anlamına gelir.
     * @return Güncellenmiş ve veritabanına kaydedilmiş {@link WalletBalance} nesnesi.
     * @throws InsufficientFundsException Eğer para çıkışı isteniyorsa ve cüzdanda yeterli bakiye yoksa.
     * @throws EntityNotFoundException    Eğer belirtilen ID'ye sahip bir cüzdan bulunamazsa.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public WalletBalance updateBalance(Long walletId, String currency, BigDecimal amountDelta)
            throws InsufficientFundsException, EntityNotFoundException {

        // Adım 1: İlgili cüzdanın var olduğundan emin ol.
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Cüzdan bulunamadı: " + walletId));

        // Adım 2: Bakiye satırını PESSIMISTIC_WRITE kilidi ile seç.
        // Bu, transaction tamamlanana kadar bu satıra başka hiçbir işlemin dokunmamasını sağlar.
        log.info("Kilit isteniyor: Cüzdan {} - Para Birimi {}", walletId, currency);
        WalletBalance balance = balanceRepository
                .findByWalletAndCurrencyForUpdate(wallet, currency)
                .orElseGet(() -> {
                    // Eğer bu para biriminde daha önce bir bakiye yoksa, sıfır bakiye ile yeni bir tane oluştur.
                    // Bu, örneğin bir cüzdana ilk kez USD yatırıldığında gerçekleşir.
                    log.info("Yeni bakiye satırı oluşturuluyor: Cüzdan {} - {}", walletId, currency);
                    WalletBalance newBalance = new WalletBalance();
                    newBalance.setWallet(wallet);
                    newBalance.setCurrency(currency);
                    newBalance.setBalance(BigDecimal.ZERO);
                    return newBalance;
                });
        log.info("Kilit alındı: Cüzdan {} - Para Birimi {}", walletId, currency);

        // Adım 3: Yeni bakiyeyi hesapla.
        BigDecimal newBalanceAmount = balance.getBalance().add(amountDelta);

        // Adım 4: Yetersiz bakiye kontrolü yap.
        // Sadece para çıkışı (amountDelta < 0) durumunda kontrol yapılır.
        if (amountDelta.compareTo(BigDecimal.ZERO) < 0 && newBalanceAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Yetersiz Bakiye: Cüzdan {}, İstenen Çıkış: {}, Mevcut Bakiye: {}", walletId, amountDelta.abs(), balance.getBalance());
            // Hata durumunda, transaction geri alınacak ve kilit serbest bırakılacaktır.
            throw new InsufficientFundsException(
                    String.format("Yetersiz Bakiye: %s ID'li cüzdanın %s para biriminde yeterli bakiyesi yok.", walletId, currency)
            );
        }

        // Adım 5: Hesaplanan yeni bakiyeyi nesne üzerinde güncelle.
        balance.setBalance(newBalanceAmount);
        
        // Adım 6: Güncellenmiş bakiye nesnesini veritabanına kaydet.
        // Transaction başarılı bir şekilde tamamlandığında, değişiklikler commit edilir ve kilit serbest bırakılır.
        WalletBalance savedBalance = balanceRepository.save(balance);
        log.info("Kilit serbest bırakıldı ve bakiye güncellendi: Cüzdan {} - Yeni Bakiye {}", walletId, newBalanceAmount);

        return savedBalance;
    }
}
