package com.nutrimate.controller;

import com.nutrimate.dto.RecentChatResponse;
import com.nutrimate.entity.ChatMessage;
import com.nutrimate.entity.ChatType;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.ChatMessageRepository;
import com.nutrimate.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Chat", description = "WebSocket chat 1-1 & lịch sử tin nhắn")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // Helper: Lấy User ID chuẩn từ Authentication (email -> Users.id)
    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện chức năng này");
        }

        String email = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt) {
            email = ((Jwt) principal).getClaimAsString("email");
        } else if (principal instanceof OidcUser) {
            email = ((OidcUser) principal).getEmail();
        } else if (principal instanceof OAuth2User) {
            email = ((OAuth2User) principal).getAttribute("email");
        }

        if (email == null) {
            throw new BadRequestException("Không tìm thấy email trong thông tin xác thực");
        }

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại trong hệ thống"));
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setId(null); // DB auto-generate
        chatMessage.setTimestamp(LocalDateTime.now());
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        String destination = saved.getChatType() == ChatType.EXPERT
                ? "/queue/expert/" + saved.getReceiverId()
                : "/queue/support/" + saved.getReceiverId();
        messagingTemplate.convertAndSend(destination, saved);
    }

    @Operation(summary = "Lấy lịch sử tin nhắn giữa 2 user theo chatType")
    @GetMapping("/api/chat/history/{user1Id}/{user2Id}")
    public List<ChatMessage> getChatHistory(
            @Parameter(description = "User 1 ID (UUID)") @PathVariable("user1Id") String user1Id,
            @Parameter(description = "User 2 ID (UUID)") @PathVariable("user2Id") String user2Id,
            @Parameter(description = "EXPERT (Tư vấn) hoặc SUPPORT (Hỗ trợ)") @RequestParam ChatType chatType) {
        return chatMessageRepository.findChatHistoryBetween(user1Id, user2Id, chatType);
    }

    @Operation(summary = "Danh sách các cuộc chat gần đây (Inbox giống Messenger/Zalo)")
    @GetMapping("/api/chat/recent")
    public List<RecentChatResponse> getRecentChats(
            @Parameter(hidden = true) Authentication authentication) {
        String currentUserId = getCurrentUserId(authentication);
        return chatMessageRepository.findRecentChatsForUser(currentUserId);
    }
}
