package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FxRequest {
    @NotBlank
    @Size(max = 100)
    private String idempotencyKey;
    @NotBlank
    private String customerId;
    @NotNull
    @Positive
    private BigDecimal amount; // Satılan miktar
    @NotBlank
    @Size(min = 3, max = 3)
    private String fromCurrency; // örn: "USD"
    @NotBlank
    @Size(min = 3, max = 3)
    private String toCurrency; // örn: "TRY"
}