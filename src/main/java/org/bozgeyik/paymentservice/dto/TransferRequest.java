
package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Idempotency key boş olamaz.")
    private String idempotencyKey;

    @NotBlank(message = "Gönderen Müşteri ID (e-posta) boş olamaz.")
    @Email(message = "Geçerli bir gönderen e-posta adresi giriniz.")
    private String fromCustomerId;

    @NotBlank(message = "Alıcı Müşteri ID (e-posta) boş olamaz.")
    @Email(message = "Geçerli bir alıcı e-posta adresi giriniz.")
    private String toCustomerId;

    @NotNull(message = "Miktar boş olamaz.")
    @DecimalMin(value = "0.01", message = "Miktar 0'dan büyük olmalıdır.")
    private BigDecimal amount;

    @NotBlank(message = "Gönderen para birimi boş olamaz.")
    private String currency;

    @NotBlank(message = "Alıcı para birimi boş olamaz.")
    private String targetCurrency;

    private String description;
}