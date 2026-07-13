package com.example.escbackend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Explicitly permit all Swagger UI & OpenAPI static resources
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                            "/uploads/**",
                                "/api/v1/payments/mpesa/b2c-result",
                                "/api/v1/payments/mpesa/b2c-timeout",
                                "/api/v1/payments/mpesa/stk-callback"
                        ).permitAll()
                        // Permit your API endpoints
                        .requestMatchers("/api/v1/**").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow local dev and ngrok tunnel origins used by Swagger UI / frontend.
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",
            "http://localhost:8080",
            "http://localhost:8081",
            "https://localhost:4200",
            "https://*.ngrok-free.dev",
            "http://*.ngrok-free.dev"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Added "ngrok-skip-browser-warning" to allow the bypass header through
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Actor-User-Id",
                "ngrok-skip-browser-warning"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Changed from "/api/v1/**" to "/**" to safeguard root/redirect requests from CORS blocks
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}