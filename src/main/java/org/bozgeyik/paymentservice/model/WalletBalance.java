package org.bozgeyik.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <-- YENİ IMPORT
import lombok.ToString; // <-- YENİ IMPORT
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "wallet_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "currency"})
)
public class WalletBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @EqualsAndHashCode.Exclude // <-- DÖNGÜYÜ KIRMAK İÇİN EKLENDİ
    @ToString.Exclude // <-- (toString için de aynı döngü olur)
    private Wallet wallet;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private Long version;
}