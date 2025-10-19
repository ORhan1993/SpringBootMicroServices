package org.bozgeyik.paymentservice.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;

/**
 * Bu servis, gerçek bir uygulamada User/Customer mikroservisine
 * bağlanarak customerId'den e-posta adresini getirir.
 * Burada test için simüle edilmiştir.
 */
@Service
public class UserService {

    // Simüle edilmiş kullanıcı veritabanı (customerId -> email)
    private static final Map<String, String> userEmailDatabase = Map.of(
            "CUSTOMER_123", "kullanici_A@mail.com",
            "CUSTOMER_456", "kullanici_B@mail.com",
            "CUSTOMER_789", "kullanici_C@mail.com"
            // Yeni cüzdan oluşturulurken buraya da kayıt atılmalı
    );

    public Optional<String> findEmailByCustomerId(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        // Simülasyon: Map'ten e-postayı bul
        return Optional.ofNullable(userEmailDatabase.get(customerId));
    }
}