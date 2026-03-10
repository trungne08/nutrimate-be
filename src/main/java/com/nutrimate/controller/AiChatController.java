package com.nutrimate.controller;

import com.nutrimate.dto.AiChatMessageDTO;
import com.nutrimate.dto.AiChatSendRequestDTO;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-chat")
@Tag(name = "AI Chat", description = "Lưu và trích xuất lịch sử chat với AI Coach")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
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
            cognitoId = ((OAuth2User) principal).getAttribute("sub");
            if (cognitoId == null) cognitoId = ((OAuth2User) principal).getName();
        }
        if (cognitoId == null || cognitoId.isBlank()) {
            throw new BadRequestException("Token không hợp lệ");
        }
        return userRepository.findByCognitoId(cognitoId)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại trong hệ thống"));
    }

    @Operation(summary = "Lấy lịch sử chat")
    @GetMapping("/history")
    public ResponseEntity<List<AiChatMessageDTO>> getChatHistory(
            @Parameter(hidden = true) Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        List<AiChatMessageDTO> history = aiChatService.getChatHistory(userId);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Gửi tin nhắn và nhận phản hồi từ AI (yêu cầu gói Premium)")
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(
            @Valid @RequestBody AiChatSendRequestDTO request,
            @Parameter(hidden = true) Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        String response = aiChatService.sendMessage(userId, request.getMessage());
        return ResponseEntity.ok(Map.of("response", response));
    }
}
