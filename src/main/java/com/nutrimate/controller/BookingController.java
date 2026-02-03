package com.nutrimate.controller;

import com.nutrimate.dto.BookingRequestDTO;
import com.nutrimate.dto.BookingStatusDTO;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Booking Management", description = "Booking APIs for Member, Expert & Admin")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

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
            cognitoId = jwt.getClaimAsString("sub");
            email = jwt.getClaimAsString("email");  
        } else if (principal instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principal;
            cognitoId = oidcUser.getName(); 
            email = oidcUser.getEmail();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            cognitoId = oauth2User.getName();
            email = oauth2User.getAttribute("email");
        }
        if (cognitoId != null) {
            Optional<User> userByCognito = userRepository.findByCognitoId(cognitoId);
            if (userByCognito.isPresent()) {
                return userByCognito.get().getId();
            }
        }
        if (email != null) {
            String finalEmail = email;
            return userRepository.findByEmail(finalEmail)
                    .map(User::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại (Email: " + finalEmail + ")"));
        }

        throw new BadRequestException("Token không hợp lệ (Không tìm thấy sub/email)");
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

    @Operation(summary = "[Admin] View all bookings")
    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings(@RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(bookingService.getAllBookings(date));
    }
}