package org.bozgeyik.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.dto.AuthRequest;
import org.bozgeyik.paymentservice.dto.AuthResponse;
import org.bozgeyik.paymentservice.dto.UserCreateRequest;
import org.bozgeyik.paymentservice.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Kayıt ol ve hemen Token üret
    public AuthResponse register(UserCreateRequest request) {
        // Mevcut UserService mantığını kullanarak kullanıcıyı oluşturuyoruz
        User user = userService.createUser(request);

        // Kullanıcı oluştu, şimdi ona bir token verelim
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token);
    }

    // Giriş yap ve Token üret
    public AuthResponse authenticate(AuthRequest request) {
        // Spring Security'nin kendi mekanizmasıyla şifre kontrolü yapıyoruz
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Eğer buraya geldiysek şifre doğrudur. Token üretip dönelim.
        String token = jwtService.generateToken(request.getEmail());
        return new AuthResponse(token);
    }
}