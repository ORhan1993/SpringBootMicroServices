package org.bozgeyik.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KALDIRILDI: customerId, ownerName ve email alanları User entity'sine taşındı.
    // YENİ: User entity'si ile ilişki kuruldu.
    // Bir cüzdan bir kullanıcıya aittir (Many-to-One).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude // Döngüsel bağımlılığı önlemek için
    private User user;

    @Column(name = "iban", unique = true, length = 34)
    private String iban;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<WalletBalance> balances = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Bu "transient" (geçici) metotlar, doğrudan User nesnesinden bilgi almayı kolaylaştırır.
    // Thymeleaf şablonlarında veya servislerde "wallet.userName" gibi ifadelere izin verir.
    @Transient
    public String getUserName() {
        return user != null ? user.getName() : null;
    }

    @Transient
    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }
}