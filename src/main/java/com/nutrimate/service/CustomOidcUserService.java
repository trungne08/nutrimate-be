package com.nutrimate.service;

import com.nutrimate.entity.SubscriptionPlan;
import com.nutrimate.entity.User;
import com.nutrimate.entity.UserBenefitUsage;
import com.nutrimate.entity.UserSubscription;
import com.nutrimate.repository.SubscriptionPlanRepository;
import com.nutrimate.repository.UserBenefitUsageRepository;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserBenefitUsageRepository benefitUsageRepository;
    
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println(">>> 🔐 ĐANG XỬ LÝ ĐĂNG NHẬP CHO USER TỪ COGNITO (OpenID Connect)...");
        
        // Gọi phương thức của lớp cha để lấy thông tin user từ Cognito
        OidcUser oidcUser = super.loadUser(userRequest);
        
        // Lấy attributes từ OidcUser (OpenID Connect)
        var claims = oidcUser.getClaims();
        System.out.println(">>> 📋 Claims từ Cognito: " + claims.keySet());
        
        // Lấy các thông tin từ Cognito claims
        String email = (String) claims.get("email");
        String cognitoId = (String) claims.get("sub"); // 'sub' là unique identifier từ Cognito
        String fullName = (String) claims.get("name");
        String username = (String) claims.getOrDefault("preferred_username", 
                          claims.getOrDefault("name", email));
        String phoneNumber = (String) claims.get("phone_number");
        String avatarUrl = (String) claims.get("picture");
        
        System.out.println(">>> 👤 Email: " + email);
        System.out.println(">>> 🆔 Cognito ID: " + cognitoId);
        System.out.println(">>> 📛 Full Name: " + fullName);
        
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (cognitoId != null) {
            cognitoId = cognitoId.trim();
        }

        // Kiểm tra user trong database (tuyệt đối không throw khi user cũ đã tồn tại)
        Optional<User> existingUser = (email == null || email.isBlank())
                ? Optional.empty()
                : userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            // User đã tồn tại -> Cập nhật thông tin từ Cognito
            System.out.println(">>> 🔄 User đã tồn tại, đang cập nhật...");
            User user = existingUser.get();
            if (cognitoId != null && !cognitoId.isBlank()) {
                // Cập nhật đè cognito_id (sub mới) để khớp với Cognito User Pool mới.
                user.setCognitoId(cognitoId);
            }
            if (fullName != null && !fullName.isEmpty()) {
                user.setFullName(fullName);
            }
            if (username != null && !username.isEmpty()) {
                user.setUsername(username);
            }
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                user.setPhoneNumber(phoneNumber);
            }
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }
            try {
                userRepository.save(user);
            } catch (Exception e) {
                // Không để lỗi DB ngăn đăng nhập thành công
                System.err.println(">>> ❌ LỖI KHI UPDATE cognitoId cho user tồn tại: " + e.getMessage());
            }
            System.out.println(">>> ✅ Đã cập nhật User: " + email);
        } else {
            // User chưa tồn tại -> Tạo mới với đầy đủ thông tin từ Cognito
            System.out.println(">>> 🆕 User mới, đang tạo...");
            User newUser = new User();
            // Nếu email/cognitoId không có, không tạo user (tránh tạo dữ liệu lỗi)
            if (email == null || email.isBlank() || cognitoId == null || cognitoId.isBlank()) {
                System.err.println(">>> ❌ Không thể tạo user mới vì thiếu email hoặc cognitoId. email=" + email + ", sub=" + cognitoId);
                return oidcUser;
            }

            newUser.setEmail(email);
            newUser.setCognitoId(cognitoId);
            // Xử lý Full Name (có thể null trong DB, nhưng nên có giá trị)
            newUser.setFullName(fullName != null && !fullName.isEmpty() 
                ? fullName 
                : (username != null && !username.isEmpty() ? username : "New Member"));
            newUser.setUsername(username != null && !username.isEmpty() ? username : email);
            newUser.setPhoneNumber(phoneNumber);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setRole(User.UserRole.MEMBER); // Đúng chuẩn ENUM trong DB
            
            try {
                User savedUser = userRepository.save(newUser);
                System.out.println(">>> ✅ Đã lưu User mới vào bảng Users: " + email);
                System.out.println(">>> 💾 User ID: " + savedUser.getId());
                assignFreeSubscription(savedUser);
            } catch (Exception e) {
                System.err.println(">>> ❌ LỖI KHI LƯU USER: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Trả về OidcUser để Spring Security tiếp tục xử lý
        return oidcUser;
    }

    private void assignFreeSubscription(User user) {
        // Logic y hệt bên trên
        try {
            SubscriptionPlan freePlan = planRepository.findFirstByPrice(BigDecimal.ZERO)
                    .orElseThrow(() -> new RuntimeException("Cấu hình lỗi: Không tìm thấy gói FREE"));

            UserSubscription sub = new UserSubscription();
            sub.setUser(user);
            sub.setPlan(freePlan);
            sub.setStartDate(LocalDateTime.now());
            sub.setEndDate(LocalDateTime.now().plusYears(100));
            sub.setStatus(UserSubscription.SubscriptionStatus.Active);
            sub.setAutoRenew(false);
            
            UserSubscription savedSub = userSubscriptionRepository.save(sub);

            UserBenefitUsage usage = new UserBenefitUsage();
            usage.setUserId(user.getId());
            usage.setSubscription(savedSub);
            usage.setDailyRecipeViews(0);
            usage.setLastRecipeViewDate(LocalDate.now());
            usage.setSessionsUsed(0);
            benefitUsageRepository.save(usage);
            System.out.println(">>> 🎁 (OIDC) Đã gán gói FREE cho user: " + user.getEmail());
        } catch (Exception e) {
            System.err.println(">>> ❌ Lỗi gán gói Free (OIDC): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
