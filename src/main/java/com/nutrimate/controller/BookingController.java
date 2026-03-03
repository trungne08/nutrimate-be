package com.nutrimate.controller;

import com.nutrimate.dto.BookingRequestDTO;
import com.nutrimate.dto.PriceCheckResponseDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "Booking Management", description = "Booking APIs for Member, Expert & Admin")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    // Helper: Lấy User ID từ sub (cognito_id) - Access Token Cognito mặc định không chứa email
    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện chức năng này");
        }

        String cognitoId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt) {
            cognitoId = ((Jwt) principal).getClaimAsString("sub");
        } else if (principal instanceof OidcUser) {
            cognitoId = ((OidcUser) principal).getSubject();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            cognitoId = oauth2User.getAttribute("sub");
            if (cognitoId == null) cognitoId = oauth2User.getName();
        }

        if (cognitoId == null || cognitoId.isBlank()) {
            throw new BadRequestException("Token không hợp lệ (Không tìm thấy sub)");
        }

        return userRepository.findByCognitoId(cognitoId)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại trong hệ thống"));
    }

    @Operation(summary = "Check price before booking")
    @PostMapping("/bookings/check")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PriceCheckResponseDTO> checkPrice(
            @RequestBody Map<String, String> request,
            @Parameter(hidden = true) Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(bookingService.checkBookingPrice(userId, request.get("expertId")));
    }

    @Operation(summary = "Create a booking")
    @PostMapping("/bookings")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Booking> createBooking(
            @RequestBody BookingRequestDTO request,
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(bookingService.createBooking(getCurrentUserId(authentication), request));
    }

    @Operation(summary = "View my booking history")
    @GetMapping("/bookings/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Booking>> getMyBookings(
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(bookingService.getMyBookings(getCurrentUserId(authentication)));
    }

    @Operation(summary = "Xem lượt đặt lịch chuyên gia miễn phí còn lại trong chu kỳ gói hiện tại")
    @GetMapping("/bookings/my-free-sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyFreeSessions(
            @Parameter(hidden = true) Authentication authentication) {

        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(bookingService.getMyFreeSessionsSummary(userId));
    }

    @Operation(summary = "[Member] Tự hủy lịch đặt của mình")
    @PutMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable String bookingId,
            @Parameter(hidden = true) Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(bookingService.cancelBookingByMember(userId, bookingId));
    }

    @Operation(summary = "[Admin] View all bookings")
    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings(@RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(bookingService.getAllBookings(date));
    }

    @Operation(summary = "Lấy chi tiết 1 lịch hẹn (Booking) theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<?> getBookingDetail(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @AuthenticationPrincipal org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {

        // 1. TÓM ĐỊNH DANH TỪ TOKEN 
        String email = null;
        String cognitoId = null;

        if (oidcUser != null) {
            email = oidcUser.getEmail();
            cognitoId = oidcUser.getSubject();
        } else if (oauth2User != null) {
            email = oauth2User.getAttribute("email");
        } else if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaimAsString("email");
            cognitoId = jwt.getSubject(); // Lấy "sub"
            if (cognitoId == null) cognitoId = jwt.getClaimAsString("username");
        }

        if ((email == null || email.trim().isEmpty()) && (cognitoId == null || cognitoId.trim().isEmpty())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập!"));
        }

        Optional<User> userOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) {
            userOpt = userRepository.findByEmail(email);
        }
        if (userOpt.isEmpty() && cognitoId != null && !cognitoId.trim().isEmpty()) {
            userOpt = userRepository.findByCognitoId(cognitoId);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Không tìm thấy User trong hệ thống!"));
        }

        User currentUser = userOpt.get();
        Booking booking = bookingService.getBookingDetail(id, currentUser.getId(), currentUser.getRole().name());
        return ResponseEntity.ok(booking);
    }
}