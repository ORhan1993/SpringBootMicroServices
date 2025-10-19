package org.bozgeyik.paymentservice.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async; // Asenkron çalışması için
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // Bu metodu @Async yaparak, uygulamanın ana akışını (thread)
    // bloklamamasını sağlıyoruz. E-posta gönderme işlemi
    // arka planda ayrı bir thread'de çalışır.
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("E-posta gönderme işlemi başlatılıyor: {} -> {}", to, subject);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setText(htmlBody, true); // true = HTML
            helper.setTo(to);
            helper.setSubject(subject);
            // helper.setFrom("no-reply@bozgeyik.com"); // properties'ten de ayarlanabilir

            mailSender.send(mimeMessage);
            log.info("E-posta başarıyla gönderildi: {}", to);
        } catch (Exception e) {
            log.error("E-posta gönderimi başarısız: {} - Hata: {}", to, e.getMessage());
        }
    }
}

// Not: Bu @Async özelliğini aktif etmek için Ana Uygulama sınıfınıza
// (PaymentServiceApplication.java) @EnableAsync eklemeniz gerekir.
// @SpringBootApplication
// @EnableAsync  <-- BU ANOTASYONU EKLEYİN
// public class PaymentServiceApplication { ... }