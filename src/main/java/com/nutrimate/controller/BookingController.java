package com.nutrimate.controller;

import com.nutrimate.dto.BookingRequestDTO;
import com.nutrimate.dto.BookingStatusDTO;
import com.nutrimate.dto.PriceCheckResponseDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.User;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api") // Base path chung, cụ thể sẽ chia ở method
@Tag(name = "5. Booking Management", description = "Booking APIs for Member, Expert & Admin")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository; // Để lấy ID từ token

    // Helper: Lấy User ID từ Token
    private String getCurrentUserId(OidcUser oidcUser, OAuth2User oauth2User) {
        String email = (oidcUser != null) ? oidcUser.getEmail() : oauth2User.getAttribute("email");
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 5.3 POST /api/bookings/check (MEMBER)
    @Operation(summary = "Check price before booking")
    @PostMapping("/bookings/check")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PriceCheckResponseDTO> checkPrice(
            @RequestBody Map<String, String> request,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(bookingService.checkBookingPrice(userId, request.get("expertId")));
    }

    // 5.4 POST /api/bookings (MEMBER)
    @Operation(summary = "Create a booking")
    @PostMapping("/bookings")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Booking> createBooking(
            @RequestBody BookingRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(bookingService.createBooking(userId, request));
    }

    // 5.5 GET /api/bookings/my-bookings (ALL)
    @Operation(summary = "View my booking history")
    @GetMapping("/bookings/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Booking>> getMyBookings(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    // 5.6 PUT /api/bookings/{id}/status (EXPERT)
    @Operation(summary = "[Expert] Confirm/Cancel booking")
    @PutMapping("/bookings/{id}/status")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<Booking> updateStatus(
            @PathVariable String id,
            @RequestBody BookingStatusDTO statusDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String expertUserId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(bookingService.updateStatus(id, expertUserId, statusDTO.getStatus()));
    }

    // 5.7 GET /api/admin/bookings (ADMIN)
    @Operation(summary = "[Admin] View all bookings")
    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(bookingService.getAllBookings(date));
    }
}