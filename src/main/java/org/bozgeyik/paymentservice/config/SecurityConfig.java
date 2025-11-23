package org.bozgeyik.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt, endüstri standardı, güçlü bir hash algoritmasıdır.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API'ler için genellikle devre dışı bırakılır
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // REST API için STATELESS
                .authorizeHttpRequests(authz -> authz
                        // DİKKAT: Bu ayar tüm endpoint'leri herkese açık hale getirir.
                        // Sadece geliştirme ve test aşamaları için kullanılmalıdır.
                        .anyRequest().permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.disable()); // Basic Auth'u da devre dışı bırakıyoruz
        return http.build();
    }
}