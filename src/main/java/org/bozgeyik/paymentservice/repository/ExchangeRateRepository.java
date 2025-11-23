package org.bozgeyik.paymentservice.repository;

import org.bozgeyik.paymentservice.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Döviz Kuru (ExchangeRate) entity'si için veritabanı işlemlerini yöneten repository arayüzü.
 * Bu repository, para birimleri arasındaki dönüşüm oranlarını saklamak ve sorgulamak için kullanılır.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * Belirtilen kaynak ve hedef para birimleri arasındaki döviz kurunu bulur.
     * Örneğin, "USD"den "TRY"ye olan dönüşüm oranını getirmek için kullanılır.
     * Spring Data JPA, metot adından yola çıkarak bu sorguyu otomatik olarak oluşturur.
     *
     * @param fromCurrency Kaynak para birimi kodu (örn: "USD").
     * @param toCurrency   Hedef para birimi kodu (örn: "TRY").
     * @return İlgili döviz kuru bilgilerini içeren bir {@link Optional<ExchangeRate>}. Eğer kur bulunamazsa, boş bir Optional döner.
     */
    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
