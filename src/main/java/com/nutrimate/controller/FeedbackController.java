package com.nutrimate.controller;

import com.nutrimate.dto.FeedbackCreateRequest;
import com.nutrimate.entity.Feedback;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository; // Dùng để gọi lấy userId

    @Operation(summary = "[Member] Gửi đánh giá cho chuyên gia sau khi khám xong")
    @PostMapping
    public ResponseEntity<Feedback> createFeedback(
            @RequestBody FeedbackCreateRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        String userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(feedbackService.createFeedback(userId, request));
    }

    @Operation(summary = "Lấy danh sách đánh giá của 1 Chuyên gia (Public)")
    @GetMapping("/expert/{expertId}")
    public ResponseEntity<?> getExpertFeedbacks(
            @PathVariable String expertId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return ResponseEntity.ok(feedbackService.getExpertFeedbacks(expertId, page, size));
    }

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        Object principal = authentication.getPrincipal();
        String cognitoId = null;

        if (principal instanceof Jwt) {
            cognitoId = ((Jwt) principal).getClaimAsString("sub");
        } else if (principal instanceof OidcUser) {
            cognitoId = ((OidcUser) principal).getName();
        } else if (principal instanceof OAuth2User) {
            cognitoId = ((OAuth2User) principal).getName();
        }

        if (cognitoId != null) {
            Optional<User> userByCognito = userRepository.findByCognitoId(cognitoId);
            if (userByCognito.isPresent()) {
                return userByCognito.get().getId();
            }
        }
        throw new BadRequestException("Token không hợp lệ");
    }
}