package org.bozgeyik.paymentservice.exception;



public class CustomAccountNotFoundException extends RuntimeException {
    public CustomAccountNotFoundException(String message) {
        super(message);
    }
}