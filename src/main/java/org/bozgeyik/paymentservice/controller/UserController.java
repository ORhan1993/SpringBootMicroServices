package org.bozgeyik.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.controller.dto.UserCreateRequest;
import org.bozgeyik.paymentservice.model.User;
import org.bozgeyik.paymentservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kullanıcı yönetimi ile ilgili API endpoint'lerini içeren controller sınıfı.
 */
@RestController
@RequestMapping("/users") // Base path for all user-related endpoints
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Yeni bir kullanıcı oluşturur.
     *
     * @param createRequest Kullanıcı oluşturma bilgilerini içeren DTO.
     * @return Oluşturulan kullanıcının bilgilerini ve HTTP 201 (Created) durumunu döner.
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateRequest createRequest) {
        User createdUser = userService.createUser(createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Belirtilen ID'ye sahip kullanıcıyı getirir.
     *
     * @param userId Getirilecek kullanıcının ID'si.
     * @return Bulunan kullanıcıyı ve HTTP 200 (OK) durumunu döner.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}
