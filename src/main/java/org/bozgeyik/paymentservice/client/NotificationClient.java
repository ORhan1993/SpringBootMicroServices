package org.bozgeyik.paymentservice.client;

import org.bozgeyik.paymentservice.client.dto.NotificationRequest;
import org.springframework.stereotype.Component;
// import org.springframework.cloud.openfeign.FeignClient;
// import org.springframework.web.bind.annotation.PostMapping;

// @FeignClient(name = "notification-service")
@Component // Feign yoksa geçici olarak Component yapalım
public class NotificationClient {
    // @PostMapping("/api/notifications")
    // void sendNotification(NotificationRequest request);

    // Geçici implementasyon
    public void sendNotification(NotificationRequest request) {
        System.out.println("--- BİLDİRİM GÖNDERİLİYOR ---");
        System.out.println("Kime: " + request.getCustomerId());
        System.out.println("Tip: " + request.getNotificationType());
        System.out.println("Mesaj: " + request.getMessage());
        System.out.println("-----------------------------");
    }
}