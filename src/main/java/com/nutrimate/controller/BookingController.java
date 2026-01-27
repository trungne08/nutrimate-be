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
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Booking Management", description = "Booking APIs for Member, Expert & Admin")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new BadRequestException("Vui lòng đăng nhập");
        String email = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) email = ((Jwt) principal).getClaimAsString("email");
        else if (principal instanceof OidcUser) email = ((OidcUser) principal).getEmail();
        else if (principal instanceof OAuth2User) email = ((OAuth2User) principal).getAttribute("email");
        
        if (email == null) throw new BadRequestException("Token không hợp lệ");
        return userRepository.findByEmail(email).map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
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

    @Operation(summary = "[Expert] Confirm/Cancel booking")
    @PutMapping("/bookings/{id}/status")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<Booking> updateStatus(
            @PathVariable String id,
            @RequestBody BookingStatusDTO statusDTO,
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(bookingService.updateStatus(id, getCurrentUserId(authentication), statusDTO.getStatus()));
    }

    @Operation(summary = "[Admin] View all bookings")
    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Booking>> getAllBookings(@RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(bookingService.getAllBookings(date));
    }
}