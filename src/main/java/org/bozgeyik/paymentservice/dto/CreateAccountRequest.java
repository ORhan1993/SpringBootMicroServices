package org.bozgeyik.paymentservice.dto;

import lombok.Data; // Veya Getter/Setter/NoArgsConstructor/AllArgsConstructor
import java.math.BigDecimal;
// Bean Validation anotasyonları eklenebilir (javax.validation.constraints.* veya jakarta.validation.constraints.*)
// import javax.validation.constraints.NotBlank;
// import javax.validation.constraints.Size;
// import javax.validation.constraints.PositiveOrZero;

@Data // Lombok'tan Getter, Setter, toString, equals, hashCode oluşturur
public class CreateAccountRequest {

    // @NotBlank(message = "Müşteri ID boş olamaz")
    private String customerId;

    // @NotBlank(message = "Hesap numarası boş olamaz")
    // @Size(min = 5, max = 20, message = "Hesap numarası 5 ile 20 karakter arasında olmalıdır")
    private String accountNumber;

    // @NotBlank(message = "Hesap sahibi adı boş olamaz")
    private String ownerName;

    // @NotBlank(message = "Para birimi boş olamaz")
    // @Size(min = 3, max = 3, message = "Para birimi 3 karakter olmalıdır (örn: TRY)")
    private String currency;

    // @PositiveOrZero(message = "Başlangıç bakiyesi negatif olamaz")
    private BigDecimal initialBalance; // İsteğe bağlı olabilir, servis varsayılan atayabilir
}
