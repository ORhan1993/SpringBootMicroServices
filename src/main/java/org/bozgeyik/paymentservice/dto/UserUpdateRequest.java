package org.bozgeyik.paymentservice.dto;


import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateRequest {

    // Güncelleme işleminde bu alan zorunlu değil
    private String name;

    // Güncelleme işleminde bu alan zorunlu değil,
    // ancak varsa formatı geçerli olmalı.
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    private String email;
}