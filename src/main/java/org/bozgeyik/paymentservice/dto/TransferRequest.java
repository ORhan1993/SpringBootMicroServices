package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import jakarta.validation.constraints.Size;
import lombok.Data;


import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank
    @Size(max = 100)
    private String idempotencyKey;
    @NotBlank
    private String fromCustomerId; // Gönderen Müşteri ID
    @NotBlank
    private String toCustomerId; // Alıcı Müşteri ID
    @NotNull
    @Positive
    private BigDecimal amount; // Gönderilecek miktar
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency; // 'amount' hangi para biriminde? (örn: "USD")
    @NotBlank
    @Size(min = 3, max = 3)
    private String targetCurrency; // Alıcı parayı hangi birimde alacak? (örn: "TRY")
    private String description;
}