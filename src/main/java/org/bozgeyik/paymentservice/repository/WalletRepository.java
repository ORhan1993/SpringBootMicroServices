package org.bozgeyik.paymentservice.repository;

import org.bozgeyik.paymentservice.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.id = :walletId")
    Optional<Wallet> findWalletWithUserById(@Param("walletId") Long walletId);

    @Query("SELECT w.id FROM Wallet w WHERE w.user.customerId = :customerId")
    List<Long> findAllWalletIdsByCustomerId(@Param("customerId") String customerId);

    boolean existsByUser_IdAndBalances_Currency(Long userId, String currency);

    Optional<Wallet> findByUser_IdAndBalances_Currency(Long userId, String currency);

    @Query("SELECT w FROM Wallet w JOIN w.user u JOIN w.balances b WHERE u.email = :email AND b.currency = :currency")
    Optional<Wallet> findByUserEmailAndCurrency(@Param("email") String email, @Param("currency") String currency);

    // YENİ EKLENEN: Kullanıcının e-postasına göre tüm cüzdanlarını getir
    @Query("SELECT w FROM Wallet w JOIN FETCH w.user u LEFT JOIN FETCH w.balances WHERE u.email = :email")
    List<Wallet> findAllByUserEmail(@Param("email") String email);
}