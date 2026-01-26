package com.nutrimate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS được xử lý bởi Spring Security (CorsConfig)
        // Config này chỉ là backup, nhưng Spring Security sẽ override
        registry.addMapping("/**")
                // Cho phép localhost và các origins cụ thể
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                // Cho phép tất cả methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                // Cho phép tất cả headers
                .allowedHeaders("*")
                // Cho phép credentials
                .allowCredentials(true)
                // Expose tất cả headers
                .exposedHeaders("*")
                // Cache preflight trong 1 giờ
                .maxAge(3600);
    }
}
