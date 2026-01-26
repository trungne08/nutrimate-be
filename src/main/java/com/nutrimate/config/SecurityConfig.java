package com.nutrimate.config;

import com.nutrimate.config.OAuth2AuthenticationSuccessHandler;
import com.nutrimate.service.CustomOAuth2UserService;
import com.nutrimate.service.CustomOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final String frontendUrl;
    private final CorsConfigurationSource corsConfigurationSource;
    
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, 
                         CustomOidcUserService customOidcUserService,
                         OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                         CorsConfigurationSource corsConfigurationSource,
                         @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.corsConfigurationSource = corsConfigurationSource;
        this.frontendUrl = frontendUrl;
        System.out.println(">>> 🔧 SecurityConfig đã được khởi tạo với CustomOAuth2UserService và CustomOidcUserService");
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Cấu hình CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Tắt CSRF để dễ test
            .csrf(csrf -> csrf.disable())
            
            // Cấu hình authorization
            .authorizeHttpRequests(auth -> auth
                // Cho phép OPTIONS requests (CORS preflight) - QUAN TRỌNG!
                .requestMatchers("OPTIONS").permitAll()
                // Cho phép truy cập công khai
                .requestMatchers("/", "/login**", "/error", 
                                "/api/auth/login", "/api/auth/status", 
                                "/oauth2/**",
                                // Swagger UI
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Các API yêu cầu xác thực
                .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/token", 
                                "/api/auth/profile", "/api/auth/profile/status", 
                                "/api/health/**").authenticated()
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
                // Sử dụng custom success handler để gửi token trong URL
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureUrl("/error")
            )
            
            // Cấu hình Logout
            .logout(logout -> logout
                // Chuyển hướng về Frontend sau khi logout thành công
                .logoutSuccessUrl(frontendUrl)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );
        
        return http.build();
    }
}
