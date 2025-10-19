package org.bozgeyik.paymentservice.event;

import lombok.Getter;
import org.bozgeyik.paymentservice.model.Transaction;
import org.springframework.context.ApplicationEvent;

/**
 * Bir finansal işlem başarıyla tamamlandığında fırlatılacak olay.
 */
@Getter
public class TransactionCompletedEvent extends ApplicationEvent {

    private final Transaction transaction;

    /**
     * @param source Olayı başlatan bileşen (genellikle 'this')
     * @param transaction Tamamlanan işlemin veritabanı kaydı
     */
    public TransactionCompletedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }
}