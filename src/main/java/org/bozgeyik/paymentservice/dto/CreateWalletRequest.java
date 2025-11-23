

package org.bozgeyik.paymentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

@Data // Getter, Setter, toString vs. için
public class CreateWalletRequest {

    @NotBlank(message = "E-posta adresi boş olamaz.")
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    private String email; // 'ownerName' veya 'customerId' DEĞİL, 'email' GEREKLİ

    @NotBlank(message = "Varsayılan para birimi boş olamaz.")
    private String defaultCurrency;

    @NotNull(message = "Başlangıç bakiyesi null olamaz (0 olabilir).")
    @PositiveOrZero(message = "Başlangıç bakiyesi negatif olamaz.")
    private BigDecimal initialBalance;
}