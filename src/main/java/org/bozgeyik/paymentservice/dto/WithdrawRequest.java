// WithdrawRequest.java
package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawRequest {
    @NotBlank
    private String idempotencyKey;
    @NotBlank
    private String customerId; // Hangi cüzdandan
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    private String currency; // örn: "TRY"
    private String iban; // Çekilecek banka hesabı
    private String description;
}
        