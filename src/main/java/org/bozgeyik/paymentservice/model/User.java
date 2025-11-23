package org.bozgeyik.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // Bu alan, servis katmanında hash'lenerek doldurulur.
    // API yanıtlarında gösterilmemelidir, bu yüzden DTO kullanımı önemlidir.
    @Column(name = "password", nullable = false)
    private String password;


     @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
     private List<Wallet> wallets = new ArrayList<>();
}