package com.nutrimate.controller;

import com.nutrimate.entity.User;
import com.nutrimate.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Authentication", description = "API endpoints for authentication with AWS Cognito")
public class AuthController {
    
    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    
    public AuthController(UserRepository userRepository, 
                         OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
    }
    
    @Operation(
            summary = "Get login URL",
            description = "Returns the OAuth2 authorization URL for Cognito login. Frontend should redirect user to this URL to start the login flow."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login URL retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"loginUrl\": \"/oauth2/authorization/cognito\", \"message\": \"Redirect to this URL to start login\"}")
                    )
            )
    })
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", "/oauth2/authorization/cognito");
        response.put("message", "Redirect to this URL to start login");
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get current user information",
            description = "Returns the authenticated user's information from database. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User information retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"authenticated\": true, \"user\": {\"id\": 1, \"email\": \"user@example.com\", \"fullName\": \"John Doe\", \"username\": \"johndoe\", \"role\": \"MEMBER\", \"avatarUrl\": \"https://...\"}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Xử lý cho OIDC (OpenID Connect) - Cognito
        if (oidcUser != null) {
            String email = oidcUser.getEmail();
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("authenticated", true);
                response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "username", user.getUsername() != null ? user.getUsername() : "",
                    "role", user.getRole().name(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
                ));
                response.put("cognitoClaims", oidcUser.getClaims());
            } else {
                response.put("authenticated", true);
                response.put("user", null);
                response.put("message", "User not found in database");
            }
        }
        // Fallback cho OAuth2 thông thường
        else if (oauth2User != null) {
            String email = oauth2User.getAttribute("email");
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                response.put("authenticated", true);
                response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "username", user.getUsername() != null ? user.getUsername() : "",
                    "role", user.getRole().name(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
                ));
            } else {
                response.put("authenticated", true);
                response.put("user", null);
            }
        } else {
            response.put("authenticated", false);
            response.put("message", "Not authenticated");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Logout user",
            description = "Returns the logout URL. Frontend should redirect user to this URL to logout. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout URL retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"logoutUrl\": \"/logout\", \"message\": \"Redirect to this URL to logout\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("logoutUrl", "/logout");
        response.put("message", "Redirect to this URL to logout");
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Check authentication status",
            description = "Checks if the current user is authenticated. Returns authentication status and email if authenticated. Public endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"authenticated\": true, \"email\": \"user@example.com\"}")
                    )
            )
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkAuthStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Map<String, Object> response = new HashMap<>();
        boolean isAuthenticated = oidcUser != null || oauth2User != null;
        response.put("authenticated", isAuthenticated);
        
        if (isAuthenticated) {
            String email = oidcUser != null ? oidcUser.getEmail() : oauth2User.getAttribute("email");
            response.put("email", email);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Get access token",
            description = "Returns the OAuth2 access token for the authenticated user. Use this token in Swagger 'Authorize' button to test protected endpoints. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Access token retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"access_token\": \"eyJraWQiOiJ...\", \"token_type\": \"Bearer\", \"id_token\": \"eyJraWQiOiJ...\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @RegisteredOAuth2AuthorizedClient("cognito") OAuth2AuthorizedClient authorizedClient) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra nếu chưa đăng nhập
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("error", "Chưa đăng nhập ní ơi!");
            response.put("message", "Vui lòng đăng nhập tại: http://localhost:8080/oauth2/authorization/cognito");
            return ResponseEntity.status(401).body(response);
        }
        
        // Lấy token từ OAuth2AuthorizedClient
        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            response.put("access_token", authorizedClient.getAccessToken().getTokenValue());
            response.put("token_type", "Bearer");
            
            // Nếu có ID token (OIDC)
            if (authorizedClient.getAccessToken().getTokenValue() != null) {
                response.put("expires_at", authorizedClient.getAccessToken().getExpiresAt());
            }
            
            // Lấy ID token nếu có (từ OidcUser)
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                if (oidcUser.getIdToken() != null) {
                    response.put("id_token", oidcUser.getIdToken().getTokenValue());
                }
            }
            
            response.put("message", "Copy access_token và dán vào Swagger 'Authorize' button");
        } else {
            // Fallback: Lấy từ OidcUser nếu có
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                if (oidcUser.getIdToken() != null) {
                    response.put("id_token", oidcUser.getIdToken().getTokenValue());
                    response.put("token_type", "Bearer");
                    response.put("message", "Đây là ID Token. Với OIDC, bạn có thể dùng ID Token để test API.");
                } else {
                    response.put("error", "Không tìm thấy token");
                    response.put("message", "Vui lòng đăng nhập lại");
                    return ResponseEntity.status(401).body(response);
                }
            } else {
                response.put("error", "Không tìm thấy token");
                response.put("message", "Vui lòng đăng nhập tại: http://localhost:8080/oauth2/authorization/cognito");
                return ResponseEntity.status(401).body(response);
            }
        }
        
        return ResponseEntity.ok(response);
    }
}
