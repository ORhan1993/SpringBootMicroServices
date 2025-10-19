package org.bozgeyik.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp; // Oluşturulma zamanı için
import org.hibernate.annotations.UpdateTimestamp;   // Güncellenme zamanı için

import java.math.BigDecimal;
import java.time.LocalDateTime; // Zaman damgaları için

@Entity
@Data
@Table(name = "accounts") // Tablo adını "accounts" olarak belirtmek iyi bir pratik
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "account_number", unique = true, nullable = false, length = 50) // Uzunluk eklenebilir
    private String accountNumber;

    @Column(name = "owner_name", nullable = false) // Hesap sahibi adı eklendi
    private String ownerName;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2) // Hassasiyet ve ölçek eklendi
    private BigDecimal balance = BigDecimal.ZERO; // Varsayılan değer ataması burada da kalabilir

    @Column(name = "currency", nullable = false, length = 3) // Para birimi için uzunluk (örn: TRY, USD)
    private String currency = "TRY"; // Varsayılan değer



    @CreationTimestamp // Otomatik olarak oluşturulma zamanını ayarlar
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Otomatik olarak her güncellemede zamanı ayarlar
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


}