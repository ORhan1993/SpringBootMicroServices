package org.bozgeyik.paymentservice.repository;

import jakarta.persistence.LockModeType;
import org.bozgeyik.paymentservice.model.Wallet;
import org.bozgeyik.paymentservice.model.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Cüzdan Bakiyesi (WalletBalance) entity'si için veritabanı işlemlerini yöneten repository arayüzü.
 * Bu repository, özellikle bakiye güncellemeleri sırasında veri tutarlılığını sağlamak için
 * pesimistik kilitleme gibi önemli mekanizmalar içerir.
 */
@Repository
public interface WalletBalanceRepository extends JpaRepository<WalletBalance, Long> {

    /**
     * Bir cüzdanın belirli bir para birimindeki bakiyesini, veritabanı seviyesinde kilitleyerek seçer.
     * `@Lock(LockModeType.PESSIMISTIC_WRITE)` annotasyonu, bu sorgu tarafından seçilen satır(lar) üzerine
     * bir 'FOR UPDATE' kilidi koyar. Bu, mevcut transaction tamamlanana kadar başka hiçbir transaction'ın
     * bu satırı okuyup değiştirememesini garanti eder. Bu, özellikle eş zamanlı para transferi gibi
     * işlemlerde "race condition"ları ve veri bozulmalarını önlemek için hayati öneme sahiptir.
     *
     * @param wallet   Kilitleme ve sorgulama yapılacak olan {@link Wallet} nesnesi.
     * @param currency Bakiyenin sorgulanacağı para birimi (örn: "TRY", "USD").
     * @return Kilitlenmiş bakiye satırını içeren bir {@link Optional<WalletBalance>}. Bakiye bulunamazsa boş Optional döner.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.wallet = :wallet AND wb.currency = :currency")
    Optional<WalletBalance> findByWalletAndCurrencyForUpdate(@Param("wallet") Wallet wallet, @Param("currency") String currency);

    /**
     * Bir cüzdanın belirli bir para birimindeki bakiyesini standart bir şekilde (kilitleme olmadan) bulur.
     * Bu metot, sadece bakiye bilgisini okumak gerektiğinde ve herhangi bir güncelleme yapılmayacağında kullanılır.
     *
     * @param wallet   Sorgulama yapılacak olan {@link Wallet} nesnesi.
     * @param currency Bakiyenin sorgulanacağı para birimi.
     * @return Bakiye bilgilerini içeren bir {@link Optional<WalletBalance>}. Bakiye bulunamazsa boş Optional döner.
     */
    Optional<WalletBalance> findByWalletAndCurrency(Wallet wallet, String currency);
}
