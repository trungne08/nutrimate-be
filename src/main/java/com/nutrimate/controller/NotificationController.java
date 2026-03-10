package com.nutrimate.controller;

import com.nutrimate.dto.NotificationDTO;
import com.nutrimate.entity.Notification;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-App thông báo và nhắc nhở")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập");
        }
        String cognitoId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) cognitoId = ((Jwt) principal).getClaimAsString("sub");
        else if (principal instanceof OidcUser) cognitoId = ((OidcUser) principal).getSubject();
        else if (principal instanceof OAuth2User) {
            cognitoId = ((OAuth2User) principal).getAttribute("sub");
            if (cognitoId == null) cognitoId = ((OAuth2User) principal).getName();
        }
        if (cognitoId == null || cognitoId.isBlank()) throw new BadRequestException("Token không hợp lệ");
        return userRepository.findByCognitoId(cognitoId)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    @Operation(summary = "Lấy danh sách thông báo (phân trang)")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> p = notificationService.getNotifications(userId, pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("content", p.getContent().stream().map(NotificationDTO::fromEntity).toList());
        result.put("totalPages", p.getTotalPages());
        result.put("totalElements", p.getTotalElements());
        result.put("currentPage", p.getNumber());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Đánh dấu một thông báo đã đọc")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        notificationService.markAsRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@Parameter(hidden = true) Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
