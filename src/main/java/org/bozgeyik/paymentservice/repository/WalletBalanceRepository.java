package org.bozgeyik.paymentservice.repository;

import jakarta.persistence.LockModeType;
import org.bozgeyik.paymentservice.model.Wallet;
import org.bozgeyik.paymentservice.model.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletBalanceRepository extends JpaRepository<WalletBalance, Long> {

    // --- BU METOT ÇOK ÖNEMLİ ---
    // Bir cüzdanın belirli bir para birimindeki bakiyesini,
    // başka bir işlem o satıra dokunamasın diye 'FOR UPDATE' (PESSIMISTIC_WRITE)
    // kilidi ile seçer.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wb FROM WalletBalance wb WHERE wb.wallet = :wallet AND wb.currency = :currency")
    Optional<WalletBalance> findByWalletAndCurrencyForUpdate(@Param("wallet") Wallet wallet, @Param("currency") String currency);

    Optional<WalletBalance> findByWalletAndCurrency(Wallet wallet, String currency);
}