package org.bozgeyik.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "from_wallet_id")
    private Long fromWalletId;

    @Column(name = "to_wallet_id")
    private Long toWalletId;

    @Column(name = "original_amount", precision = 19, scale = 4)
    private BigDecimal originalAmount; // örn: 100

    @Column(name = "original_currency", length = 3)
    private String originalCurrency; // örn: "USD"

    @Column(name = "converted_amount", precision = 19, scale = 4)
    private BigDecimal convertedAmount; // örn: 3300

    @Column(name = "target_currency", length = 3)
    private String targetCurrency; // örn: "TRY"

    @Column(name = "exchange_rate_used", precision = 19, scale = 8)
    private BigDecimal exchangeRateUsed;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @CreationTimestamp
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;
}