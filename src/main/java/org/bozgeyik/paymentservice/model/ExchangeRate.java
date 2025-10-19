package org.bozgeyik.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "exchange_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency"})
)
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency;

    @Column(name = "rate", precision = 19, scale = 8, nullable = false) // Yüksek kur hassasiyeti
    private BigDecimal rate;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}