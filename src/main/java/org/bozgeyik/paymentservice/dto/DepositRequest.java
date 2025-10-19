
package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotBlank
    private String idempotencyKey;
    @NotBlank
    private String customerId; // Hangi cüzdana
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    private String currency; // örn: "TRY"
    private String paymentGatewayToken; // örn: Stripe, BKM token'ı
    private String description;
}
        