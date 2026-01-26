package com.nutrimate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
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
        
        // Tạo danh sách origins cho phép
        List<String> origins = new ArrayList<>();
        
        // Thêm localhost (cho dev)
        origins.add("http://localhost:5173");
        origins.add("http://127.0.0.1:5173");
        origins.add("http://localhost:3000");
        origins.add("http://127.0.0.1:3000");
        
        // Thêm frontend URL từ config
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            origins.add(frontendUrl);
        }
        
        // Thêm các origins từ properties (nếu có)
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            String[] originsArray = allowedOrigins.split(",");
            for (String origin : originsArray) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }
        
        // Nếu không có origins nào, dùng pattern "*" (chỉ khi không có credentials)
        if (origins.isEmpty()) {
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
            configuration.setAllowCredentials(false); // Phải tắt credentials khi dùng "*"
        } else {
            configuration.setAllowedOrigins(origins);
            configuration.setAllowCredentials(true); // Cho phép credentials với origins cụ thể
        }
        
        // Cho phép tất cả methods (QUAN TRỌNG: phải có OPTIONS cho preflight)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // Cho phép tất cả headers (bao gồm Authorization, Content-Type, etc.)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Expose tất cả headers cho frontend (bao gồm Authorization, Set-Cookie, etc.)
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        // Cache preflight requests trong 1 giờ
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng CORS cho TẤT CẢ các endpoints
        source.registerCorsConfiguration("/**", configuration);
        
        System.out.println(">>> 🌐 CORS Config - Allowed Origins: " + origins);
        
        return source;
    }
}
