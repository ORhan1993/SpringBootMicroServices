package org.bozgeyik.paymentservice.model;

public enum TransactionType {
    DEPOSIT,    // Cüzdana dışarıdan para yükleme
    WITHDRAWAL, // Cüzdandan dışarıya para çekme
    TRANSFER,   // Cüzdanlar arası transfer
    FX_TRADE    // Cüzdan içi döviz alım/satım
}