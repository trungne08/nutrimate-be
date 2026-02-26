package com.nutrimate.controller;

import com.nutrimate.dto.SystemFeedbackRequest;
import com.nutrimate.entity.SystemFeedback;
import com.nutrimate.entity.User;
import com.nutrimate.repository.SystemFeedbackRepository;
import com.nutrimate.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/system-feedbacks")
@Tag(name = "System Feedback", description = "Đánh giá toàn bộ hệ thống Nutrimate")
@RequiredArgsConstructor
public class SystemFeedbackController {

    private final SystemFeedbackRepository systemFeedbackRepository;
    private final UserRepository userRepository;

    @Operation(summary = "1. Gửi đánh giá hệ thống (Bắt buộc đăng nhập)")
    @PostMapping
    public ResponseEntity<?> submitFeedback(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User,
            @Valid @RequestBody SystemFeedbackRequest request) {

        // --- ĐOẠN CODE BẮT TOKEN BẤT BẠI ---
        String email = null;
        String cognitoId = null;

        if (oidcUser != null) {
            email = oidcUser.getEmail();
            cognitoId = oidcUser.getSubject();
        } else if (oauth2User != null) {
            email = oauth2User.getAttribute("email");
        } else if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
            email = jwt.getClaimAsString("email");
            cognitoId = jwt.getSubject();
            if (cognitoId == null) cognitoId = jwt.getClaimAsString("username");
        }

        if ((email == null || email.trim().isEmpty()) && (cognitoId == null || cognitoId.trim().isEmpty())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập để đánh giá!"));
        }

        Optional<User> userOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() && cognitoId != null && !cognitoId.trim().isEmpty()) userOpt = userRepository.findByCognitoId(cognitoId);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Không tìm thấy User!"));
        }

        // --- LƯU ĐÁNH GIÁ VÀO DB ---
        SystemFeedback feedback = new SystemFeedback();
        feedback.setUser(userOpt.get());
        feedback.setRating(request.getRating());
        feedback.setContent(request.getContent());

        systemFeedbackRepository.save(feedback);

        return ResponseEntity.ok(Map.of("success", true, "message", "Cảm ơn bạn đã đánh giá Nutrimate!"));
    }

    @Operation(summary = "2. Xem danh sách đánh giá hệ thống (Public - Ai cũng xem được)")
    @GetMapping
    public ResponseEntity<?> getSystemFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SystemFeedback> feedbackPage = systemFeedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
        Double avgRating = systemFeedbackRepository.getAverageSystemRating();

        Map<String, Object> response = new HashMap<>();
        // Định hình lại data trả về cho Frontend dễ đọc
        response.put("feedbacks", feedbackPage.getContent().stream().map(fb -> Map.of(
                "id", fb.getId(),
                "userName", fb.getUser().getFullName() != null ? fb.getUser().getFullName() : "Người dùng ẩn danh",
                "userAvatar", fb.getUser().getAvatarUrl() != null ? fb.getUser().getAvatarUrl() : "",
                "rating", fb.getRating(),
                "content", fb.getContent() != null ? fb.getContent() : "",
                "createdAt", fb.getCreatedAt()
        )));
        response.put("currentPage", feedbackPage.getNumber());
        response.put("totalItems", feedbackPage.getTotalElements());
        response.put("totalPages", feedbackPage.getTotalPages());
        response.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        return ResponseEntity.ok(response);
    }
}