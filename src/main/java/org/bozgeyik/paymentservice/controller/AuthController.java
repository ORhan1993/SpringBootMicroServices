package org.bozgeyik.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.dto.AuthRequest;
import org.bozgeyik.paymentservice.dto.AuthResponse;
import org.bozgeyik.paymentservice.dto.UserCreateRequest;
import org.bozgeyik.paymentservice.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;

    // Kayıt Endpoint'i: /auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Giriş Endpoint'i: /auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}