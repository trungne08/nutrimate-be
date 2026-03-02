package com.nutrimate.controller;

import com.nutrimate.dto.BookingStatusDTO;
import com.nutrimate.dto.ExpertApplicationDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.ExpertProfile;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.BookingService;
import com.nutrimate.service.ExpertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/experts")
@Tag(name = "Expert Profile", description = "Public APIs to find experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    // 5.1 GET /api/experts (Filter)
    @Operation(summary = "Search experts")
    @GetMapping
    public ResponseEntity<List<ExpertProfile>> searchExperts(
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(expertService.searchExperts(minRating, maxPrice));
    }

    // 5.2 GET /api/experts/{id}
    @Operation(summary = "Get expert profile")
    @GetMapping("/{id}")
    public ResponseEntity<ExpertProfile> getExpertDetail(@PathVariable String id) {
        return ResponseEntity.ok(expertService.getExpertById(id));
    }

    @Operation(summary = "[Expert] Xem danh sách booking của mình")
    @GetMapping("/my-bookings")
    public ResponseEntity<List<Booking>> getMyBookings(Authentication authentication) {
        // 👇 Thêm dòng này để test
        System.out.println("DEBUG: Đã vào được Controller /my-bookings"); 

        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(expertService.getMyExpertBookings(userId));
    }

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện chức năng này");
        }

        Object principal = authentication.getPrincipal();
        String cognitoId = null;
        String email = null;

        // 1. Lấy thông tin từ Token
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            cognitoId = jwt.getClaimAsString("sub"); // Lấy UUID (luôn có)
            email = jwt.getClaimAsString("email");   // Có thể null trong Access Token
        } else if (principal instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principal;
            cognitoId = oidcUser.getName(); // Thường là sub
            email = oidcUser.getEmail();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            cognitoId = oauth2User.getName();
            email = oauth2User.getAttribute("email");
        }

        // 2. Ưu tiên tìm bằng Cognito ID (Chính xác nhất)
        if (cognitoId != null) {
            Optional<User> userByCognito = userRepository.findByCognitoId(cognitoId);
            if (userByCognito.isPresent()) {
                return userByCognito.get().getId();
            }
        }

        // 3. Nếu không tìm thấy bằng Cognito ID thì mới fallback sang Email
        if (email != null) {
            String finalEmail = email; // biến final để dùng trong lambda
            return userRepository.findByEmail(finalEmail)
                    .map(User::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại (Email: " + finalEmail + ")"));
        }

        throw new BadRequestException("Token không hợp lệ (Không tìm thấy sub/email)");
    }
    
    @Operation(summary = "Apply to become an Expert")
    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ExpertProfile> applyToBecomeExpert(
            @RequestParam("specialization") String specialization,
            @RequestParam("bio") String bio,
            @RequestParam("yearsExperience") Integer yearsExperience,
            @RequestParam("hourlyRate") BigDecimal hourlyRate,
            @RequestParam(value = "certificateFile", required = false) MultipartFile certificateFile,
            @Parameter(hidden = true) Authentication authentication) {

        ExpertApplicationDTO dto = new ExpertApplicationDTO();
        dto.setSpecialization(specialization);
        dto.setBio(bio);
        dto.setYearsExperience(yearsExperience);
        dto.setHourlyRate(hourlyRate);
        // File sẽ được xử lý riêng trong Service

        String userId = getCurrentUserId(authentication);
        
        return ResponseEntity.ok(expertService.submitApplication(userId, dto, certificateFile));
    }

    @Operation(summary = "[Expert] Cập nhật trạng thái lịch hẹn")
    @PutMapping("/bookings/{bookingId}/status")
    public ResponseEntity<Booking> updateStatus(
            @PathVariable String bookingId,
            @RequestBody BookingStatusDTO req, // DTO chứa status + note
            @Parameter(hidden = true) Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        // Gọi hàm service đã nâng cấp
        return ResponseEntity.ok(bookingService.updateStatus(userId, bookingId, req));
    }

    @Operation(summary = "Lấy danh sách khung giờ trống của Expert theo ngày")
    @GetMapping("/{expertId}/availability")
    public ResponseEntity<Map<String, Object>> getExpertAvailability(
            @PathVariable String expertId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<String> availableSlots = bookingService.getExpertAvailableSlots(expertId, date);
        return ResponseEntity.ok(Map.of(
                "date", date,
                "availableSlots", availableSlots
        ));
    }
}