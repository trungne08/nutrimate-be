package com.nutrimate.controller;

import com.nutrimate.dto.AiCoachChatRequestDTO;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.AiCoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "AI Coach", description = "Chat với AI Coach (Python Microservice)")
@RequiredArgsConstructor
public class AiCoachController {

    private final AiCoachService aiCoachService;
    private final UserRepository userRepository;

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để sử dụng AI Coach");
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
            throw new BadRequestException("Token không hợp lệ");
        }

        return userRepository.findByCognitoId(cognitoId)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại trong hệ thống"));
    }

    @Operation(summary = "Chat với AI Coach")
    @PostMapping("/ai-coach/chat")
    public ResponseEntity<Map<String, String>> chat(
            @Valid @RequestBody AiCoachChatRequestDTO request,
            @Parameter(hidden = true) Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        String response = aiCoachService.getAdviceFromAi(userId, request.getMessage());
        return ResponseEntity.ok(Map.of("response", response));
    }
}
