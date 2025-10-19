package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAccountRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String ownerName;
    // Örneğin hesap durumu (ACTIVE, FROZEN vb.) de güncellenebilir
    // private AccountStatus status;
}
