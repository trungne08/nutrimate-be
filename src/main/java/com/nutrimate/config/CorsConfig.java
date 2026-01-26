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
        
        // MỞ FULL CORS - Cho phép tất cả origins
        // Dùng setAllowedOriginPatterns thay vì setAllowedOrigins để có thể dùng "*" với credentials
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Cho phép tất cả methods (QUAN TRỌNG: phải có OPTIONS cho preflight)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // Cho phép tất cả headers (bao gồm Authorization, Content-Type, etc.)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Cho phép credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Expose tất cả headers cho frontend (bao gồm Authorization, Set-Cookie, etc.)
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // Cache preflight requests trong 1 giờ
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng CORS cho TẤT CẢ các endpoints
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
