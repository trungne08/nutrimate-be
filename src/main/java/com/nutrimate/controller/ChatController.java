package com.nutrimate.controller;

import com.nutrimate.entity.ChatMessage;
import com.nutrimate.entity.ChatType;
import com.nutrimate.repository.ChatMessageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@Tag(name = "Chat", description = "WebSocket chat 1-1 & lịch sử tin nhắn")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

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
}
