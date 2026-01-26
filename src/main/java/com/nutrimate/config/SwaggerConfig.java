package com.nutrimate.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Value("${app.backend.url:}")
    private String backendUrl;
    
    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        
        // Server tự động detect từ request hiện tại (Render/Railway/localhost)
        // SpringDoc sẽ tự động resolve relative URL thành absolute URL
        servers.add(new Server()
                .url("/")
                .description("Current Server (Auto-detected)"));
        
        // Thêm localhost nếu đang chạy local (để có thể test local)
        servers.add(new Server()
                .url("http://localhost:8080")
                .description("Local Development (if running locally)"));
        
        // Thêm server từ biến môi trường nếu có (backup)
        if (backendUrl != null && !backendUrl.isEmpty() && !backendUrl.equals("http://localhost:8080")) {
            String productionUrl = backendUrl;
            // Đảm bảo dùng HTTPS cho production
            if (productionUrl.startsWith("http://") && !productionUrl.contains("localhost")) {
                productionUrl = productionUrl.replace("http://", "https://");
            }
            servers.add(new Server()
                    .url(productionUrl)
                    .description("Production Server (from config)"));
        }
        
        return new OpenAPI()
                .info(new Info()
                        .title("Nutrimate API Documentation")
                        .version("1.0.0")
                        .description("API documentation for Nutrimate application with AWS Cognito authentication")
                        .contact(new Contact()
                                .name("Nutrimate Team")
                                .email("support@nutrimate.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(servers)
                // Cấu hình nút Authorize (Ổ khóa) trong Swagger UI
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }
    
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Nhập Access Token từ endpoint /api/auth/token. Format: Bearer {token}");
    }
}
