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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserBenefitUsageRepository benefitUsageRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println(">>> 🔐 ĐANG XỬ LÝ ĐĂNG NHẬP CHO USER TỪ COGNITO...");
        
        // Gọi phương thức của lớp cha để lấy thông tin user từ Cognito
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        // Lấy attributes từ OAuth2User
        Map<String, Object> attributes = oauth2User.getAttributes();
        System.out.println(">>> 📋 Attributes từ Cognito: " + attributes.keySet());
        
        // Lấy các thông tin từ Cognito attributes
        String email = (String) attributes.get("email");
        String cognitoId = (String) attributes.get("sub"); // 'sub' là unique identifier từ Cognito
        String fullName = (String) attributes.get("name");
        String username = (String) attributes.getOrDefault("preferred_username", 
                          attributes.getOrDefault("name", email));
        String phoneNumber = (String) attributes.get("phone_number");
        String avatarUrl = (String) attributes.get("picture");
        
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
                return oauth2User;
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
                System.out.println(">>> 💾 User ID: " + newUser.getId());
                assignFreeSubscription(savedUser);
            } catch (Exception e) {
                System.err.println(">>> ❌ LỖI KHI LƯU USER: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Trả về OAuth2User để Spring Security tiếp tục xử lý
        return oauth2User;
    }

    private void assignFreeSubscription(User user) {
        try {
            SubscriptionPlan freePlan = planRepository.findFirstByPrice(BigDecimal.ZERO)
                    .orElseThrow(() -> new RuntimeException("Chưa có gói FREE trong DB"));

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
            System.out.println(">>> 🎁 Đã tặng gói FREE cho: " + user.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
