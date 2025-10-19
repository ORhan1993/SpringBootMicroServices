package org.bozgeyik.paymentservice.repository;

import org.bozgeyik.paymentservice.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByCustomerId(String customerId);
    boolean existsByCustomerId(String customerId);
}