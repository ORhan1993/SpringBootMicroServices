package org.bozgeyik.paymentservice.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    // Frontend'den gelen isteği karşılayacak metod
    @PostMapping("/create")
    public Map<String, String> createCustomer(@RequestBody Map<String, Object> customerData) {

        // Konsola yazdıralım ki isteğin geldiğini görelim
        System.out.println("Gelen Müşteri Verisi: " + customerData);

        // Şimdilik veritabanına kaydetmiş gibi yapıp başarılı cevabı dönelim
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Müşteri Kaydı Başarılı: " + customerData.get("name"));

        return response;
    }
}