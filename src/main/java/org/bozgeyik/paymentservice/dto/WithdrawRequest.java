// WithdrawRequest.java
package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawRequest {
    @NotBlank(message = "Hesap numarası boş olamaz")
    private String accountNumber;

    @NotNull(message = "Miktar boş olamaz")
    @DecimalMin(value = "0.01", message = "Miktar pozitif olmalıdır")
    private BigDecimal amount;

    @Size(max = 3, message = "Para birimi en fazla 3 karakter olabilir")
    private String currency; // İsteğe bağlı, servis varsayılan atayabilir

    private String description; // İsteğe bağlı
}
        