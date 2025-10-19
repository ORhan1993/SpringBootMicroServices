package org.bozgeyik.paymentservice.dto;



import lombok.extern.slf4j.Slf4j;
import org.bozgeyik.paymentservice.client.NotificationClient;
import org.springframework.stereotype.Component;

@Component // Spring tarafından yönetilmesi için
@Slf4j
public class NotificationClientFallback implements NotificationClient {

    @Override
    public void sendNotification(NotificationRequest notificationRequest) {
        // Bu metot, NotificationClient'taki sendNotification metodu için
        // devre kesici açık olduğunda veya bir hata oluştuğunda çağrılır.
        log.error("FALLBACK: Notification service is unavailable or an error occurred. " +
                        "Could not send notification for customer: {}, type: {}, message: '{}'",
                notificationRequest.getCustomerId(),
                notificationRequest.getNotificationType(),
                notificationRequest.getMessage());

        // Burada alternatif bir işlem yapabilirsiniz:
        // 1. Mesajı bir "dead letter queue" (DLQ) veya başka bir kalıcı depoya kaydetmek
        //    ve daha sonra tekrar denemek üzere bir mekanizma kurmak.
        // 2. Kullanıcıya bir uyarı mesajı göstermek (eğer senkron bir işlemse).
        // 3. Sadece loglamak ve işlemi devam ettirmek (mevcut TransactionService'teki gibi).
        //    Bu durumda, bildirim gönderilemese bile ana işlem (örn: para transferi) tamamlanmış olur.

        // Örnek: Sadece loglama yapılıyor.
        // Gerçek bir uygulamada, bu mesajları bir veritabanına veya mesaj kuyruğuna
        // yazarak daha sonra işlenmesini sağlamak daha iyi bir pratik olabilir.
    }
}
