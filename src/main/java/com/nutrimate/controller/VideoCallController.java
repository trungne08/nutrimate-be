package com.nutrimate.controller;

import com.nutrimate.util.TokenServerAssistant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/video-call")
@Tag(name = "Video Call", description = "Zego Video Call token API")
public class VideoCallController {

    @Value("${ZEGO_APP_ID:0}")
    private long appId;

    @Value("${ZEGO_SERVER_SECRET:}")
    private String serverSecret;

    @Operation(summary = "Get Zego token for video call")
    @GetMapping("/token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getToken(
            @Parameter(description = "User ID (unique per app)", required = true)
            @RequestParam String userId,
            @Parameter(description = "Room ID (optional, empty for basic auth)")
            @RequestParam(required = false, defaultValue = "") String roomId) {

        if (appId == 0 || serverSecret == null || serverSecret.isEmpty()) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "ZEGO_APP_ID hoặc ZEGO_SERVER_SECRET chưa được cấu hình"));
        }

        try {
            // Token sống 3600s (1 tiếng), payload rỗng cho basic auth
            String payload = "";
            TokenServerAssistant.TokenInfo tokenInfo = TokenServerAssistant.generateToken04(
                    appId,
                    userId,
                    serverSecret,
                    3600,
                    payload
            );

            if (tokenInfo.error.code != TokenServerAssistant.ErrorCode.SUCCESS) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", tokenInfo.error.message));
            }

            return ResponseEntity.ok(Map.of(
                    "token", tokenInfo.data,
                    "appId", appId
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi tạo token: " + e.getMessage()));
        }
    }
}
