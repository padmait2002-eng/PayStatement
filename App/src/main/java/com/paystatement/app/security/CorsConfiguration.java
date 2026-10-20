package com.paystatement.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfiguration {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${paystatement.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4200,http://127.0.0.1:4200}") String configuredOrigins) {
        var configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(configuredOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Employee-Id", "X-User-Role"));
        configuration.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
