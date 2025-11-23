
package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FxRequest {

    @NotBlank(message = "Idempotency key boş olamaz.")
    private String idempotencyKey;

    @NotBlank(message = "Müşteri ID (e-posta) boş olamaz.")
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    private String customerId;

    @NotNull(message = "Miktar boş olamaz.")
    @DecimalMin(value = "0.01", message = "Miktar 0'dan büyük olmalıdır.")
    private BigDecimal amount;

    @NotBlank(message = "Kaynak para birimi boş olamaz.")
    private String fromCurrency;

    @NotBlank(message = "Hedef para birimi boş olamaz.")
    private String toCurrency;
}