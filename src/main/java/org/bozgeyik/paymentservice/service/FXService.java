package org.bozgeyik.paymentservice.service;

import jakarta.annotation.PostConstruct; // Bu import gerekli
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.model.ExchangeRate;
import org.bozgeyik.paymentservice.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FXService {

    private final ExchangeRateRepository rateRepository;

    // --- BLOK GÜNCELLENDİ ---
    @PostConstruct
    public void initRates() {
        if (rateRepository.count() == 0) {
            // Hatalı constructor kullanımı yerine setter'ları kullanıyoruz

            ExchangeRate rate1 = new ExchangeRate();
            rate1.setFromCurrency("USD");
            rate1.setToCurrency("TRY");
            rate1.setRate(new BigDecimal("33.50"));
            rateRepository.save(rate1);

            ExchangeRate rate2 = new ExchangeRate();
            rate2.setFromCurrency("TRY");
            rate2.setToCurrency("USD");
            rate2.setRate(new BigDecimal("0.0298"));
            rateRepository.save(rate2);

            ExchangeRate rate3 = new ExchangeRate();
            rate3.setFromCurrency("EUR");
            rate3.setToCurrency("TRY");
            rate3.setRate(new BigDecimal("36.20"));
            rateRepository.save(rate3);

            ExchangeRate rate4 = new ExchangeRate();
            rate4.setFromCurrency("TRY");
            rate4.setToCurrency("EUR");
            rate4.setRate(new BigDecimal("0.0276"));
            rateRepository.save(rate4);

            ExchangeRate rate5 = new ExchangeRate();
            rate5.setFromCurrency("USD");
            rate5.setToCurrency("EUR");
            rate5.setRate(new BigDecimal("0.92"));
            rateRepository.save(rate5);

            ExchangeRate rate6 = new ExchangeRate();
            rate6.setFromCurrency("EUR");
            rate6.setToCurrency("USD");
            rate6.setRate(new BigDecimal("1.08"));
            rateRepository.save(rate6);
        }
    }
    // --- GÜNCELLEME SONU ---

    public BigDecimal getRate(String from, String to) {
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }
        return rateRepository.findByFromCurrencyAndToCurrency(from, to)
                .map(ExchangeRate::getRate)
                .orElseThrow(() -> new EntityNotFoundException("Kur bulunamadı: " + from + " -> " + to));
    }

    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (from.equals(to)) {
            return amount;
        }
        BigDecimal rate = getRate(from, to);
        // Uluslararası scale=4 ve bankacı yuvarlaması
        return amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);
    }
}