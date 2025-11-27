package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExternalTransferRequest {

    @NotBlank(message = "Idempotency key boş olamaz.")
    private String idempotencyKey;

    @NotBlank(message = "Gönderen Müşteri E-postası boş olamaz.")
    private String fromCustomerId;

    @NotBlank(message = "Alıcı IBAN boş olamaz.")
    private String toIban;

    @NotBlank(message = "Alıcı SWIFT kodu boş olamaz.")
    private String swiftCode;

    @NotBlank(message = "Alıcı Adı Soyadı boş olamaz.")
    private String receiverName;

    @NotNull(message = "Miktar boş olamaz.")
    @DecimalMin(value = "1.00", message = "Dış transfer için minimum miktar 1.00 olmalıdır.")
    private BigDecimal amount;

    @NotBlank(message = "Para birimi boş olamaz.")
    private String currency;

    private String description;
}