package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.Email; // <-- YENİ IMPORT
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateWalletRequest {
    @NotBlank
    private String customerId;

    @NotBlank
    @Size(min = 3)
    private String ownerName;

    // --- YENİ EKLENDİ ---
    @NotBlank
    @Email // E-posta formatını doğrular
    private String email;
    // --- BİTTİ ---

    @NotBlank
    @Size(min = 3, max = 3)
    private String defaultCurrency; // örn: "TRY"

    @NotNull
    private BigDecimal initialBalance; // 0 da olabilir, @Positive olmamalı
}