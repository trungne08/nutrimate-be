package com.nutrimate.controller;

import com.nutrimate.dto.DailyLogResponseDTO;
import com.nutrimate.dto.TrackingRequestDTO;
import com.nutrimate.entity.User;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.TrackingService;
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

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Tracking", description = "Food & Meal Logging APIs")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final UserRepository userRepository;

    // Helper lấy UserID
    private String getCurrentUserId(OidcUser oidcUser, OAuth2User oauth2User) {
        String email = (oidcUser != null) ? oidcUser.getEmail() : oauth2User.getAttribute("email");
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 7.1 GET /api/tracking/{date}
    @Operation(summary = "Get food log by date (yyyy-MM-dd)")
    @GetMapping("/{date}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<DailyLogResponseDTO> getLogByDate(
            @PathVariable LocalDate date,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(trackingService.getLogByDate(userId, date));
    }

    // 7.2 POST /api/tracking/log
    @Operation(summary = "Add food to log")
    @PostMapping("/log")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<DailyLogResponseDTO> addFoodLog(
            @RequestBody TrackingRequestDTO.AddFoodLog request,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(trackingService.addFoodLog(userId, request));
    }

    // 7.3 PUT /api/tracking/log/{id}
    @Operation(summary = "Update food amount")
    @PutMapping("/log/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<DailyLogResponseDTO> updateFoodLog(
            @PathVariable String id,
            @RequestBody TrackingRequestDTO.UpdateFoodLog request,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(trackingService.updateFoodLog(userId, id, request));
    }

    // 7.4 DELETE /api/tracking/log/{id}
    @Operation(summary = "Delete food log")
    @DeleteMapping("/log/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> deleteFoodLog(
            @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        trackingService.deleteFoodLog(userId, id);
        return ResponseEntity.ok("Deleted successfully");
    }
}