package org.bozgeyik.paymentservice.repository;

import org.bozgeyik.paymentservice.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Idempotency kontrolü için
    boolean existsByIdempotencyKey(String idempotencyKey);

    // İşlem dökümü için (Pageable destekli)
    Page<Transaction> findByFromWalletIdOrToWalletId(Long fromWalletId, Long toWalletId, Pageable pageable);
}