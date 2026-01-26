package com.nutrimate.config;

import com.nutrimate.service.CustomOAuth2UserService;
import com.nutrimate.service.CustomOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, 
                         CustomOidcUserService customOidcUserService) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        System.out.println(">>> 🔧 SecurityConfig đã được khởi tạo với CustomOAuth2UserService và CustomOidcUserService");
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF để dễ test
            .csrf(csrf -> csrf.disable())
            
            // Cấu hình authorization
            .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập công khai
                .requestMatchers("/", "/login**", "/error", 
                                "/api/auth/login", "/api/auth/status", 
                                "/oauth2/**",
                                // Swagger UI
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Các API yêu cầu xác thực
                .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/token").authenticated()
                // Các trang khác yêu cầu xác thực
                .anyRequest().authenticated()
            )
            
            // Cấu hình OAuth2 Login
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> {
                    System.out.println(">>> 🔗 Đang cấu hình userInfoEndpoint...");
                    // Với OpenID Connect (scope=openid), dùng oidcUserService
                    userInfo.oidcUserService(customOidcUserService);
                    // Fallback cho OAuth2 thông thường
                    userInfo.userService(customOAuth2UserService);
                })
                // Chuyển hướng về Frontend sau khi login thành công
                .defaultSuccessUrl("http://localhost:5173", true)
                .failureUrl("/error")
            );
        
        return http.build();
    }
}
