package com.nutrimate.controller;

import com.nutrimate.dto.UpdateProfileRequest;
import com.nutrimate.entity.HealthProfile;
import com.nutrimate.entity.User;
import com.nutrimate.entity.UserSubscription;
import com.nutrimate.repository.UserSubscriptionRepository;
import com.nutrimate.repository.HealthProfileRepository;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.FileUploadService;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API endpoints for authentication with AWS Cognito")
public class AuthController {
    
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final FileUploadService fileUploadService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final String backendUrl;
    private final String frontendUrl;
    private final String sessionCookieSameSite;
    private final boolean sessionCookieSecure;
    private final UserSubscriptionRepository userSubscriptionRepository;
    
    public AuthController(UserRepository userRepository, 
                         HealthProfileRepository healthProfileRepository,
                         FileUploadService fileUploadService,
                         ClientRegistrationRepository clientRegistrationRepository,
                         UserSubscriptionRepository userSubscriptionRepository,
                         @Value("${app.backend.url:http://localhost:8080}") String backendUrl,
                         @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
                         @Value("${server.servlet.session.cookie.same-site:lax}") String sessionCookieSameSite,
                         @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure) {
        this.userRepository = userRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.fileUploadService = fileUploadService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.backendUrl = backendUrl;
        this.frontendUrl = frontendUrl;
        this.sessionCookieSameSite = sessionCookieSameSite;
        this.sessionCookieSecure = sessionCookieSecure;
        this.userSubscriptionRepository = userSubscriptionRepository;
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
    public ResponseEntity<Map<String, String>> getLoginUrl(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        
        // Tự động detect URL từ request hiện tại (Railway/Render/localhost)
        String baseUrl;
        try {
            // Dùng ServletUriComponentsBuilder để tự động detect scheme, host, port từ request
            baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(null) // Xóa path hiện tại
                    .scheme(request.getScheme()) // Giữ nguyên scheme (http/https)
                    .build()
                    .toUriString();
        } catch (Exception e) {
            // Fallback về backendUrl từ config nếu không detect được
            baseUrl = backendUrl;
        }
        
        // Đảm bảo dùng HTTPS cho production (Railway luôn dùng HTTPS)
        if (baseUrl.startsWith("http://") && !baseUrl.contains("localhost")) {
            baseUrl = baseUrl.replace("http://", "https://");
        }
        
        String loginUrl = baseUrl + "/oauth2/authorization/cognito";
        response.put("loginUrl", loginUrl);
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
                            examples = @ExampleObject(value = "{\"authenticated\": true, \"user\": {\"id\": 1, \"email\": \"user@example.com\", \"fullName\": \"John Doe\", \"username\": \"johndoe\", \"phoneNumber\": \"+84901234567\", \"role\": \"MEMBER\", \"avatarUrl\": \"https://...\"}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = Optional.empty();
        
        // 1. TÌM USER TỪ MỌI NGUỒN TOKEN (JWT / OIDC / OAuth2)
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String email = jwt.getClaimAsString("email");
            if (email == null) email = jwt.getClaimAsString("cognito:username");
            if (email == null) email = jwt.getClaimAsString("preferred_username");
            
            if (email != null) userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty() && jwt.getClaimAsString("sub") != null) {
                userOpt = userRepository.findByCognitoId(jwt.getClaimAsString("sub"));
            }
        } else if (oidcUser != null) {
            userOpt = userRepository.findByEmail(oidcUser.getEmail());
            response.put("cognitoClaims", oidcUser.getClaims());
        } else if (oauth2User != null) {
            userOpt = userRepository.findByEmail(oauth2User.getAttribute("email"));
        } else {
            response.put("authenticated", false);
            response.put("message", "Not authenticated");
            return ResponseEntity.ok(response);
        }
        
        // 2. BUILD DATA TRẢ VỀ CHO FRONTEND
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            response.put("authenticated", true);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            userInfo.put("username", user.getUsername() != null ? user.getUsername() : "");
            userInfo.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            userInfo.put("role", user.getRole().name());
            userInfo.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
            
            // 🚀 ĐOẠN MỚI: TÌM VÀ GẮN GÓI SUBSCRIPTION VÀO RESPONSE
            Optional<UserSubscription> activeSub = userSubscriptionRepository.findActiveSubscriptionByUserId(user.getId());
            if (activeSub.isPresent()) {
                UserSubscription sub = activeSub.get();
                Map<String, Object> subInfo = new HashMap<>();
                subInfo.put("planName", sub.getPlan().getPlanName());
                subInfo.put("status", sub.getStatus().name());
                subInfo.put("startDate", sub.getStartDate());
                subInfo.put("endDate", sub.getEndDate());
                
                // Tiện tay check luôn xem có phải gói xịn không cho FE dễ code if-else
                String planUp = sub.getPlan().getPlanName().toUpperCase();
                subInfo.put("isPremium", planUp.contains("PREMIUM") || planUp.contains("EXPERT"));
                
                userInfo.put("subscription", subInfo);
            } else {
                userInfo.put("subscription", null); // Không có gói thì trả null (Free User)
            }
            
            response.put("user", userInfo);
        } else {
            response.put("authenticated", true);
            response.put("user", null);
            response.put("message", "User not found in database");
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
    public ResponseEntity<Map<String, Object>> logout(
            HttpServletRequest request,
            HttpServletResponse httpServletResponse,
            @Parameter(hidden = true) Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        // 1) Logout local (invalidate session + clear SecurityContext)
        new SecurityContextLogoutHandler().logout(request, httpServletResponse, authentication);

        // 2) Clear cookies để tránh "logout xong login lại auto vào account cũ"
        clearCookie(httpServletResponse, "JSESSIONID", true);
        clearCookie(httpServletResponse, "XSRF-TOKEN", false);
        clearCookie(httpServletResponse, "csrf-state-legacy", false);

        // 3) (Optional) Tạo Cognito logout URL để xoá luôn phiên SSO ở Hosted UI
        String cognitoLogoutUrl = buildCognitoLogoutUrl();

        response.put("success", true);
        response.put("message", "Logged out successfully");
        response.put("redirectUrl", frontendUrl);
        if (cognitoLogoutUrl != null) {
            response.put("cognitoLogoutUrl", cognitoLogoutUrl);
        }
        return ResponseEntity.ok(response);
    }

    private void clearCookie(HttpServletResponse response, String name, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(httpOnly)
                .secure(sessionCookieSecure)
                .sameSite(sessionCookieSameSite)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String buildCognitoLogoutUrl() {
        try {
            if (!(clientRegistrationRepository instanceof InMemoryClientRegistrationRepository)) {
                return null;
            }
            ClientRegistration reg = ((InMemoryClientRegistrationRepository) clientRegistrationRepository)
                    .findByRegistrationId("cognito");
            if (reg == null) {
                return null;
            }

            String authorizationUri = reg.getProviderDetails().getAuthorizationUri();
            if (authorizationUri == null || authorizationUri.isBlank()) {
                return null;
            }

            // Ví dụ auth uri: https://{domain}.auth.{region}.amazoncognito.com/oauth2/authorize
            String base = UriComponentsBuilder.fromUriString(authorizationUri)
                    .replacePath("/logout")
                    .replaceQuery(null)
                    .build()
                    .toUriString();

            return UriComponentsBuilder.fromUriString(base)
                    .queryParam("client_id", reg.getClientId())
                    .queryParam("logout_uri", frontendUrl)
                    .build()
                    .toUriString();
        } catch (Exception e) {
            return null;
        }
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
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Map<String, Object> response = new HashMap<>();
        boolean isAuthenticated = oidcUser != null || oauth2User != null;

        // Hỗ trợ Bearer access_token (JwtAuthenticationToken)
        if (!isAuthenticated && authentication instanceof JwtAuthenticationToken jwtAuth) {
            isAuthenticated = true;
            Jwt jwt = jwtAuth.getToken();

            // Ưu tiên lấy email từ claim nếu có
            String email = jwt.getClaimAsString("email");

            // Nếu không có email trong token, tra DB theo cognitoId (sub)
            if (email == null) {
                String sub = jwt.getClaimAsString("sub");
                if (sub != null) {
                    userRepository.findByCognitoId(sub).ifPresent(user -> {
                        response.put("email", user.getEmail());
                    });
                }
            } else {
                response.put("email", email);
            }
        }

        response.put("authenticated", isAuthenticated);
        
        // Nếu đã có email ở trên (Bearer token hoặc logic khác) thì không cần set lại
        if (isAuthenticated && !response.containsKey("email")) {
            String email = null;
            if (oidcUser != null) {
                email = oidcUser.getEmail();
            } else if (oauth2User != null) {
                email = oauth2User.getAttribute("email");
            }
            if (email != null) {
                response.put("email", email);
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Check profile completion status",
            description = "Checks if the authenticated user's profile and health profile are complete. " +
                         "Returns missing fields for both profiles. Frontend should use this to determine " +
                         "if user needs to complete their profile. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"userProfile\": {\"complete\": false, \"missingFields\": [\"fullName\", \"phoneNumber\"]}, \"healthProfile\": {\"complete\": false, \"missingFields\": [\"gender\", \"dateOfBirth\", \"heightCm\", \"weightKg\"]}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            )
    })
    @GetMapping("/profile/status")
    public ResponseEntity<Map<String, Object>> checkProfileStatus(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        Map<String, Object> response = new HashMap<>();

        // Lấy thông tin định danh từ authenticated user (hỗ trợ cả session và Bearer access_token)
        String email = null;
        String cognitoId = null;

        if (oidcUser != null) {
            email = oidcUser.getEmail();
            cognitoId = oidcUser.getSubject();
        } else if (oauth2User != null) {
            email = oauth2User.getAttribute("email");
        } else if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();
            email = jwt.getClaimAsString("email");
            cognitoId = jwt.getClaimAsString("sub");
        }

        if ((email == null || email.trim().isEmpty()) && (cognitoId == null || cognitoId.trim().isEmpty())) {
            response.put("success", false);
            response.put("message", "Unauthorized - Please login first");
            return ResponseEntity.status(401).body(response);
        }
        
        // Tìm user trong database
        Optional<User> userOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) {
            userOpt = userRepository.findByEmail(email);
        }
        if (userOpt.isEmpty() && cognitoId != null && !cognitoId.trim().isEmpty()) {
            userOpt = userRepository.findByCognitoId(cognitoId);
        }
        
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found in database");
            return ResponseEntity.status(404).body(response);
        }
        
        User user = userOpt.get();
        
        // Kiểm tra User Profile
        Map<String, Object> userProfileStatus = new HashMap<>();
        java.util.List<String> missingUserFields = new java.util.ArrayList<>();
        
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            missingUserFields.add("fullName");
        }
        if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
            missingUserFields.add("phoneNumber");
        }
        
        userProfileStatus.put("complete", missingUserFields.isEmpty());
        userProfileStatus.put("missingFields", missingUserFields);
        
        // Kiểm tra Health Profile
        Map<String, Object> healthProfileStatus = new HashMap<>();
        java.util.List<String> missingHealthFields = new java.util.ArrayList<>();
        
        Optional<HealthProfile> healthProfileOpt = healthProfileRepository.findByUserId(user.getId());
        
        if (healthProfileOpt.isEmpty()) {
            // Chưa có health profile
            healthProfileStatus.put("exists", false);
            healthProfileStatus.put("complete", false);
            // Chỉ các trường bắt buộc: gender, dateOfBirth, heightCm, weightKg
            missingHealthFields.add("gender");
            missingHealthFields.add("dateOfBirth");
            missingHealthFields.add("heightCm");
            missingHealthFields.add("weightKg");
            // activityLevel và dietaryPreference là optional, không thêm vào missingFields
            healthProfileStatus.put("missingFields", missingHealthFields);
        } else {
            HealthProfile healthProfile = healthProfileOpt.get();
            healthProfileStatus.put("exists", true);
            
            // Kiểm tra các trường BẮT BUỘC để tính toán (không bao gồm activityLevel và dietaryPreference)
            if (healthProfile.getGender() == null) {
                missingHealthFields.add("gender");
            }
            if (healthProfile.getDateOfBirth() == null) {
                missingHealthFields.add("dateOfBirth");
            }
            if (healthProfile.getHeightCm() == null || healthProfile.getHeightCm() <= 0) {
                missingHealthFields.add("heightCm");
            }
            if (healthProfile.getWeightKg() == null || healthProfile.getWeightKg() <= 0) {
                missingHealthFields.add("weightKg");
            }
            // activityLevel và dietaryPreference là optional, không kiểm tra
            
            healthProfileStatus.put("complete", missingHealthFields.isEmpty());
            healthProfileStatus.put("missingFields", missingHealthFields);
        }
        
        // Tổng hợp kết quả
        boolean allComplete = userProfileStatus.get("complete").equals(true) && 
                            healthProfileStatus.get("complete").equals(true);
        
        response.put("success", true);
        response.put("allComplete", allComplete);
        response.put("userProfile", userProfileStatus);
        response.put("healthProfile", healthProfileStatus);
        
        if (!allComplete) {
            response.put("message", "Profile is incomplete. Please complete missing fields.");
        } else {
            response.put("message", "Profile is complete.");
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
            response.put("message", "Vui lòng đăng nhập tại: " + backendUrl + "/oauth2/authorization/cognito");
            return ResponseEntity.status(401).body(response);
        }
        
        // Lấy token từ OAuth2AuthorizedClient
        if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
            response.put("access_token", authorizedClient.getAccessToken().getTokenValue());
            response.put("token_type", "Bearer");
            
            // Thông tin về thời gian hết hạn
            if (authorizedClient.getAccessToken().getExpiresAt() != null) {
                response.put("expires_at", authorizedClient.getAccessToken().getExpiresAt());
                
                // Tính số giây còn lại (expires_in)
                long expiresInSeconds = java.time.Instant.now()
                        .until(authorizedClient.getAccessToken().getExpiresAt(), java.time.temporal.ChronoUnit.SECONDS);
                response.put("expires_in", expiresInSeconds);
                
                // Thông tin dễ đọc hơn
                if (expiresInSeconds > 0) {
                    long minutes = expiresInSeconds / 60;
                    long hours = minutes / 60;
                    if (hours > 0) {
                        response.put("expires_in_human", hours + " giờ " + (minutes % 60) + " phút");
                    } else {
                        response.put("expires_in_human", minutes + " phút");
                    }
                } else {
                    response.put("expires_in_human", "Token đã hết hạn");
                }
            }
            
            // Lấy refresh token nếu có
            if (authorizedClient.getRefreshToken() != null) {
                response.put("refresh_token", authorizedClient.getRefreshToken().getTokenValue());
                if (authorizedClient.getRefreshToken().getExpiresAt() != null) {
                    response.put("refresh_token_expires_at", authorizedClient.getRefreshToken().getExpiresAt());
                }
            }
            
            // Lấy ID token nếu có (từ OidcUser)
            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                if (oidcUser.getIdToken() != null) {
                    response.put("id_token", oidcUser.getIdToken().getTokenValue());
                    if (oidcUser.getIdToken().getExpiresAt() != null) {
                        response.put("id_token_expires_at", oidcUser.getIdToken().getExpiresAt());
                    }
                }
            }
            
            response.put("message", "Copy access_token và dán vào Swagger 'Authorize' button. Token sẽ hết hạn sau " + 
                    (response.containsKey("expires_in_human") ? response.get("expires_in_human") : "một khoảng thời gian") + ".");
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
                response.put("message", "Vui lòng đăng nhập tại: " + backendUrl + "/oauth2/authorization/cognito");
                return ResponseEntity.status(401).body(response);
            }
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Refresh access token",
            description = "Refreshes the access token using refresh token. Use this endpoint when access token expires. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated or refresh token expired"
            )
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @RegisteredOAuth2AuthorizedClient("cognito") OAuth2AuthorizedClient authorizedClient) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("error", "Chưa đăng nhập!");
            response.put("message", "Vui lòng đăng nhập tại: " + backendUrl + "/oauth2/authorization/cognito");
            return ResponseEntity.status(401).body(response);
        }
        
        // Kiểm tra có refresh token không
        if (authorizedClient == null || authorizedClient.getRefreshToken() == null) {
            response.put("error", "Không có refresh token");
            response.put("message", "Vui lòng đăng nhập lại để lấy refresh token mới");
            return ResponseEntity.status(401).body(response);
        }
        
        // Kiểm tra refresh token có hết hạn chưa
        if (authorizedClient.getRefreshToken().getExpiresAt() != null) {
            if (authorizedClient.getRefreshToken().getExpiresAt().isBefore(java.time.Instant.now())) {
                response.put("error", "Refresh token đã hết hạn");
                response.put("message", "Vui lòng đăng nhập lại");
                return ResponseEntity.status(401).body(response);
            }
        }
        
        // Lấy access token mới (Spring Security sẽ tự động refresh nếu cần)
        if (authorizedClient.getAccessToken() != null) {
            response.put("access_token", authorizedClient.getAccessToken().getTokenValue());
            response.put("token_type", "Bearer");
            
            if (authorizedClient.getAccessToken().getExpiresAt() != null) {
                response.put("expires_at", authorizedClient.getAccessToken().getExpiresAt());
                long expiresInSeconds = java.time.Instant.now()
                        .until(authorizedClient.getAccessToken().getExpiresAt(), java.time.temporal.ChronoUnit.SECONDS);
                response.put("expires_in", expiresInSeconds);
                
                long minutes = expiresInSeconds / 60;
                long hours = minutes / 60;
                if (hours > 0) {
                    response.put("expires_in_human", hours + " giờ " + (minutes % 60) + " phút");
                } else {
                    response.put("expires_in_human", minutes + " phút");
                }
            }
            
            response.put("message", "Token đã được refresh thành công");
        } else {
            response.put("error", "Không thể refresh token");
            response.put("message", "Vui lòng đăng nhập lại");
            return ResponseEntity.status(401).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
            summary = "Update user profile",
            description = "Updates the authenticated user's profile information (fullName, username, phoneNumber, avatar). " +
                         "All fields are optional - only provided fields will be updated. " +
                         "Avatar can be uploaded as a file (multipart/form-data) or provided as URL. " +
                         "If avatarFile is provided, it will be uploaded to Cloudinary and the URL will be saved. " +
                         "User can only update their own profile. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"success\": true, \"message\": \"Profile updated successfully\", \"user\": {\"id\": 1, \"email\": \"user@example.com\", \"fullName\": \"John Doe\", \"username\": \"johndoe\", \"phoneNumber\": \"+84901234567\", \"role\": \"MEMBER\", \"avatarUrl\": \"https://...\"}}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Validation error or upload failed"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - User not authenticated"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found in database"
            )
    })
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User,
            @Valid @ModelAttribute UpdateProfileRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (oidcUser == null && oauth2User == null) {
            response.put("success", false);
            response.put("message", "Unauthorized - Please login first");
            return ResponseEntity.status(401).body(response);
        }
        
        // Lấy email từ authenticated user
        String email = oidcUser != null ? oidcUser.getEmail() : oauth2User.getAttribute("email");
        
        // Tìm user trong database
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found in database");
            return ResponseEntity.status(404).body(response);
        }
        
        User user = userOpt.get();
        
        // Cập nhật các trường (chỉ cập nhật nếu có giá trị mới)
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            user.setUsername(request.getUsername().trim());
        }
        
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        
        // Xử lý avatar: ưu tiên upload file, nếu không có file thì dùng URL
        try {
            if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
                // Upload file lên Cloudinary
                String avatarUrl = fileUploadService.uploadFile(request.getAvatarFile());
                user.setAvatarUrl(avatarUrl);
            } else if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
                // Dùng URL trực tiếp (backward compatible)
                user.setAvatarUrl(request.getAvatarUrl().trim());
            }
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error uploading avatar: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
        
        // Lưu vào database
        try {
            userRepository.save(user);
            
            // Trả về thông tin user đã cập nhật
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            userInfo.put("username", user.getUsername() != null ? user.getUsername() : "");
            userInfo.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            userInfo.put("role", user.getRole().name());
            userInfo.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
            
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", userInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating profile: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
}
