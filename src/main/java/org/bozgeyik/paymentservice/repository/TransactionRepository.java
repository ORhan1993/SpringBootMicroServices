package org.bozgeyik.paymentservice.repository;



import org.bozgeyik.paymentservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountOrToAccountOrderByTransactionDateDesc(String fromAccount, String toAccount);

}