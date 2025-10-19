package org.bozgeyik.paymentservice.client;



import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.bozgeyik.paymentservice.dto.NotificationClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// "notification-service" -> application.yml/properties dosyasında Feign client'ın
// bağlanacağı servisin adıdır (Eureka'da kayıtlı adı veya direkt URL olabilir).
// fallback = NotificationClientFallback.class -> Devre kesici için fallback sınıfını belirtir.
@FeignClient(name = "notification-service", fallback = NotificationClientFallback.class)
public interface NotificationClient {

    // application.yml dosyasındaki circuitbreaker instance adı ile aynı olmalı: "notificationService"
    // Fallback metodu, fallback sınıfında (NotificationClientFallback) tanımlanacak.
    @PostMapping("/api/notifications/send") // Bildirim servisindeki endpoint yolu
    @CircuitBreaker(name = "notificationService") // Fallback metodu fallback sınıfından çağrılacak
    void sendNotification(@RequestBody org.bozgeyik.paymentservice.dto.NotificationRequest notificationRequest);
}
