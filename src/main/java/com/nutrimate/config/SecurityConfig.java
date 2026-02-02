package com.nutrimate.config;
import com.nutrimate.service.CustomOAuth2UserService;
import com.nutrimate.service.CustomOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
            // Tắt CSRF
            .csrf(csrf -> csrf.disable())
            
            // Cấu hình authorization
            .authorizeHttpRequests(auth -> auth
                // 1. Cho phép OPTIONS requests (quan trọng cho CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 2. Các API Public (Login, Auth, Swagger)
                .requestMatchers("/", "/login**", "/error", 
                                "/api/auth/login", "/api/auth/status", 
                                "/oauth2/**",
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                
                .requestMatchers(HttpMethod.GET, "/api/forum/posts/**", "/api/forum/comments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/experts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/challenges/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/recipe/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/plans/**").permitAll()
                .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/token", 
                                "/api/auth/profile", "/api/auth/profile/status", 
                                "/api/health/**").authenticated()
                                
                // 5. Tất cả các request còn lại phải xác thực
                .anyRequest().authenticated()
            )
            
            // Cấu hình OAuth2 Login (Để chuyển hướng sang Google/Cognito login)
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> {
                    userInfo.oidcUserService(customOidcUserService);
                    userInfo.userService(customOAuth2UserService);
                })
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureUrl("/error")
            )
            
            // 👇 QUAN TRỌNG: Cấu hình Resource Server để nhận Bearer Token từ Swagger/Postman
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            
            // Cấu hình Logout
            .logout(logout -> logout
                .logoutSuccessUrl(frontendUrl)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
            );
        
        return http.build();
    }
}
