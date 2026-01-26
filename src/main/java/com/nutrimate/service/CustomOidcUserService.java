package com.nutrimate.service;

import com.nutrimate.entity.User;
import com.nutrimate.repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomOidcUserService extends OidcUserService {
    
    private final UserRepository userRepository;
    
    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        System.out.println(">>> 🎯 CustomOidcUserService đã được khởi tạo!");
    }
    
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
        
        // Kiểm tra user trong database
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            // User đã tồn tại -> Cập nhật thông tin từ Cognito
            System.out.println(">>> 🔄 User đã tồn tại, đang cập nhật...");
            User user = existingUser.get();
            user.setCognitoId(cognitoId);
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
            userRepository.save(user);
            System.out.println(">>> ✅ Đã cập nhật User: " + email);
        } else {
            // User chưa tồn tại -> Tạo mới với đầy đủ thông tin từ Cognito
            System.out.println(">>> 🆕 User mới, đang tạo...");
            User newUser = new User();
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
                userRepository.save(newUser);
                System.out.println(">>> ✅ Đã lưu User mới vào bảng Users: " + email);
                System.out.println(">>> 💾 User ID: " + newUser.getId());
            } catch (Exception e) {
                System.err.println(">>> ❌ LỖI KHI LƯU USER: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Trả về OidcUser để Spring Security tiếp tục xử lý
        return oidcUser;
    }
}
