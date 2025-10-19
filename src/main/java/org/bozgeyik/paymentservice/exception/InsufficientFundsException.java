package org.bozgeyik.paymentservice.exception;

public class InsufficientFundsException extends RuntimeException { // Veya extends Exception, projenizin hata yönetimi stratejisine bağlı olarak

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}