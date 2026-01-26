package com.nutrimate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Cho phép cả frontend URL từ config và localhost (để dev local)
        List<String> origins = Arrays.asList(
            frontendUrl,
            "http://localhost:5173",
            "http://127.0.0.1:5173"
        );
        
        // Nếu có thêm allowed origins từ env, thêm vào
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            origins.addAll(Arrays.asList(allowedOrigins.split(",")));
        }
        
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
