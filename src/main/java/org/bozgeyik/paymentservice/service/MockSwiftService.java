package org.bozgeyik.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class MockSwiftService {



    /**
     * Dış bankaya transfer işlemini simüle eder.
     * @return İşlem başarılıysa true, başarısızsa false döner.
     */
    public boolean processTransfer(String iban, String swiftCode, String receiverName, double amount) {
        log.info("--- SWIFT TRANSFER BAŞLATILIYOR ---");
        log.info("Hedef Banka: {} | IBAN: {} | Alıcı: {}", swiftCode, iban, receiverName);
        log.info("Tutar: {}", amount);

        try {
            // Gerçekçi olması için 2 ile 4 saniye arası bekletelim (Network gecikmesi)
            log.info("SWIFT ağına bağlanılıyor...");
            Thread.sleep(2000 + new Random().nextInt(2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // %15 ihtimalle işlemin başarısız olmasını sağlayalım (Simülasyon)
        if (new Random().nextInt(100) < 15) {
            log.error("HATA: Karşı banka yanıt vermedi veya işlem reddedildi.");
            return false;
        }

        log.info("BAŞARILI: Transfer karşı banka tarafından onaylandı.");
        log.info("Referans No: {}", UUID.randomUUID().toString());
        return true;
    }
}