package org.bozgeyik.paymentservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter; // <-- YENİ IMPORT
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitMQConfig {

    // E-posta işlemleri için kuyruk adı
    public static final String QUEUE_NAME = "email-notification-queue";

    @Bean
    public Queue emailQueue() {
        // durable=true: RabbitMQ çökse bile kuyruk silinmez
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}