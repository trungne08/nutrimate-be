package com.nutrimate.controller;

import com.nutrimate.dto.ChallengeDTO;
import com.nutrimate.dto.UserChallengeDTO;
import com.nutrimate.entity.Challenge;
import com.nutrimate.entity.User;
import com.nutrimate.entity.UserChallenge;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.ChallengeService;
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
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Community & Challenges", description = "Challenge APIs")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;
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

    @Operation(summary = "List all available challenges")
    @GetMapping("/challenges")
    public ResponseEntity<List<Challenge>> getAllChallenges() {
        return ResponseEntity.ok(challengeService.getAllChallenges());
    }

    // --- ADMIN ---
    @Operation(summary = "[Admin] Create new challenge (multipart/form-data: title, description, durationDays, level, imageFile)")
    @PostMapping(value = "/admin/challenges", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Challenge> createChallenge(@Valid @ModelAttribute ChallengeDTO.CreateRequest req) throws java.io.IOException {
        return ResponseEntity.ok(challengeService.createChallenge(req));
    }

    @Operation(summary = "[Admin] Update challenge (multipart/form-data: title, description, durationDays, level, imageFile)")
    @PutMapping(value = "/admin/challenges/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Challenge> updateChallenge(
            @PathVariable String id,
            @Valid @ModelAttribute ChallengeDTO.CreateRequest req) throws java.io.IOException {
        return ResponseEntity.ok(challengeService.updateChallenge(id, req));
    }

    @Operation(summary = "[Admin] Delete challenge")
    @DeleteMapping("/admin/challenges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteChallenge(@PathVariable String id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.ok("Challenge deleted successfully");
    }

    // --- MEMBER ---
    @Operation(summary = "Join a challenge")
    @PostMapping("/challenges/{id}/join")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<?> joinChallenge(@PathVariable String id, Authentication auth) {
        String userId = getCurrentUserId(auth);
        challengeService.joinChallenge(userId, id);
        return ResponseEntity.ok(Map.of("message", "Participated in the challenge successfully!"));
    }

    @Operation(summary = "View my joined challenges & progress")
    @GetMapping("/challenges/my-challenges")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<ChallengeDTO.Response>> getMyChallenges(Authentication auth) {
        String userId = getCurrentUserId(auth);
        return ResponseEntity.ok(challengeService.getMyChallenges(userId));
    }

    @PostMapping("/challenges/{id}/check-in")
    public ResponseEntity<?> checkIn(@PathVariable String id, Authentication auth) {
        String userId = getCurrentUserId(auth);
        challengeService.checkInChallenge(userId, id);
        return ResponseEntity.ok(Map.of("message", "Registration successful! Keep going!"));
    }
}