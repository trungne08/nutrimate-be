package com.nutrimate.controller;

import com.nutrimate.dto.ChallengeDTO;
import com.nutrimate.entity.Challenge;
import com.nutrimate.entity.User;
import com.nutrimate.entity.UserChallenge;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.ChallengeService;
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

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "8. Community & Challenges", description = "Challenge APIs")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UserRepository userRepository;

    // Helper lấy UserID
    private String getCurrentUserId(OidcUser oidcUser, OAuth2User oauth2User) {
        String email = (oidcUser != null) ? oidcUser.getEmail() : oauth2User.getAttribute("email");
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 8.1 GET /api/challenges (ALL)
    @Operation(summary = "List all available challenges")
    @GetMapping("/challenges")
    public ResponseEntity<List<Challenge>> getAllChallenges() {
        return ResponseEntity.ok(challengeService.getAllChallenges());
    }

    // 8.2 POST /api/admin/challenges (ADMIN)
    @Operation(summary = "[Admin] Create new challenge")
    @PostMapping("/admin/challenges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Challenge> createChallenge(@RequestBody ChallengeDTO.CreateRequest req) {
        return ResponseEntity.ok(challengeService.createChallenge(req));
    }

    // 8.3 PUT /api/admin/challenges/{id} (ADMIN)
    @Operation(summary = "[Admin] Update challenge")
    @PutMapping("/admin/challenges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Challenge> updateChallenge(
            @PathVariable String id, 
            @RequestBody ChallengeDTO.CreateRequest req) {
        return ResponseEntity.ok(challengeService.updateChallenge(id, req));
    }

    // 8.4 DELETE /api/admin/challenges/{id} (ADMIN)
    @Operation(summary = "[Admin] Delete challenge")
    @DeleteMapping("/admin/challenges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteChallenge(@PathVariable String id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.ok("Challenge deleted successfully");
    }

    // 8.5 POST /api/challenges/{id}/join (MEMBER)
    @Operation(summary = "Join a challenge")
    @PostMapping("/challenges/{id}/join")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<UserChallenge> joinChallenge(
            @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(challengeService.joinChallenge(userId, id));
    }

    // 8.6 GET /api/challenges/my-challenges (MEMBER)
    @Operation(summary = "View my joined challenges & progress")
    @GetMapping("/challenges/my-challenges")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<List<ChallengeDTO.Response>> getMyChallenges(
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = getCurrentUserId(oidcUser, oauth2User);
        return ResponseEntity.ok(challengeService.getMyChallenges(userId));
    }
}