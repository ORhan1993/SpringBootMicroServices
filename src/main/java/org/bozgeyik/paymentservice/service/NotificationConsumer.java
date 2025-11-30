package org.bozgeyik.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bozgeyik.paymentservice.config.RabbitMQConfig;
import org.bozgeyik.paymentservice.dto.EmailMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;

    // Kuyrukta mesaj var mı diye dinler
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeEmailMessage(EmailMessage message) {
        log.info("Kuyruktan mesaj alındı, e-posta gönderiliyor: {}", message.getTo());

        // Gerçek mail atma işlemi burada yapılır
        emailService.sendHtmlEmail(message.getTo(), message.getSubject(), message.getBody());
    }
}