package com.nutrimate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Cho phép tất cả origins
                .allowedOriginPatterns("*")
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
