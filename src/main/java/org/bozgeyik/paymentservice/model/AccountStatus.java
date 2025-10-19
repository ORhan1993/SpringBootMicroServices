package org.bozgeyik.paymentservice.model;

public enum AccountStatus {
    ACTIVE,  // Aktif, tüm işlemlere açık
    CLOSED,  // Kapatılmış, işlem yapılamaz
    FROZEN   // Dondurulmuş, geçici olarak işlemlere kapalı
}
