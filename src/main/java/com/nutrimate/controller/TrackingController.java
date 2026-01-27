package com.nutrimate.controller;

import com.nutrimate.dto.DailyLogResponseDTO;
import com.nutrimate.dto.TrackingRequestDTO;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.TrackingService;
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

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Tracking", description = "Food & Meal Logging APIs")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final UserRepository userRepository;

    // Helper
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

    @Operation(summary = "Get food log by date (yyyy-MM-dd)")
    @GetMapping("/{date}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<DailyLogResponseDTO> getLogByDate(
            @PathVariable LocalDate date,
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(trackingService.getLogByDate(getCurrentUserId(authentication), date));
    }

    @Operation(summary = "Add food to log")
    @PostMapping("/log")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<DailyLogResponseDTO> addFoodLog(
            @RequestBody TrackingRequestDTO.AddFoodLog request,
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(trackingService.addFoodLog(getCurrentUserId(authentication), request));
    }

    @Operation(summary = "Update food amount")
    @PutMapping("/log/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<DailyLogResponseDTO> updateFoodLog(
            @PathVariable String id,
            @RequestBody TrackingRequestDTO.UpdateFoodLog request,
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(trackingService.updateFoodLog(getCurrentUserId(authentication), id, request));
    }

    @Operation(summary = "Delete food log")
    @DeleteMapping("/log/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> deleteFoodLog(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        trackingService.deleteFoodLog(getCurrentUserId(authentication), id);
        return ResponseEntity.ok("Deleted successfully");
    }
}