package org.bozgeyik.paymentservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.model.ExchangeRate;
import org.bozgeyik.paymentservice.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Döviz Kuru (Foreign Exchange) işlemlerini yöneten servis sınıfı.
 * Bu servis, para birimleri arasındaki dönüşüm oranlarını sağlar ve tutar dönüştürme işlemlerini gerçekleştirir.
 */
@Service
@RequiredArgsConstructor
public class FXService {

    private final ExchangeRateRepository rateRepository;

    /**
     * Belirtilen para birimleri arasındaki dönüşüm oranını (kurunu) getirir.
     *
     * @param from Kaynak para birimi kodu (örn: "USD").
     * @param to   Hedef para birimi kodu (örn: "TRY").
     * @return Dönüşüm oranı olarak {@link BigDecimal}.
     * @throws EntityNotFoundException Eğer belirtilen kur veritabanında bulunamazsa.
     */
    public BigDecimal getRate(String from, String to) {
        // Eğer kaynak ve hedef para birimi aynı ise, dönüşüm oranı 1'dir.
        if (from.equalsIgnoreCase(to)) {
            return BigDecimal.ONE;
        }
        // Veritabanından kuru bul ve döndür. Bulunamazsa hata fırlat.
        return rateRepository.findByFromCurrencyAndToCurrency(from, to)
                .map(ExchangeRate::getRate)
                .orElseThrow(() -> new EntityNotFoundException("Dönüşüm kuru bulunamadı: " + from + " -> " + to));
    }

    /**
     * Belirtilen bir tutarı, bir para biriminden diğerine dönüştürür.
     *
     * @param amount Dönüştürülecek para tutarı.
     * @param from   Kaynak para birimi kodu.
     * @param to     Hedef para birimi kodu.
     * @return Dönüştürülmüş ve yuvarlanmış para tutarı.
     */
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        // Eğer para birimleri aynı ise, herhangi bir dönüşüm yapmaya gerek yok.
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        // İlgili kuru al.
        BigDecimal rate = getRate(from, to);
        
        // Tutarı kur ile çarp ve sonucu yuvarla.
        // Finansal hesaplamalarda genellikle `HALF_EVEN` (Banker's Rounding) kullanılır.
        // Bu yuvarlama modu, istatistiksel olarak daha az yanlıdır.
        // 4 ondalık basamak hassasiyeti (scale) çoğu finansal sistem için standart bir yaklaşımdır.
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_EVEN);
    }


}
