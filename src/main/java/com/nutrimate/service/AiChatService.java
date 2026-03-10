package com.nutrimate.service;

import com.nutrimate.dto.AiChatMessageDTO;
import com.nutrimate.dto.AiCoachRequestDTO;
import com.nutrimate.entity.AiChatMessage;
import com.nutrimate.entity.AiChatMessage.MessageRole;
import com.nutrimate.entity.User;
import com.nutrimate.entity.UserSubscription.SubscriptionStatus;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.AiChatMessageRepository;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private static final int CONTEXT_MESSAGE_LIMIT = 10;

    private final AiChatMessageRepository aiChatMessageRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final AiCoachService aiCoachService;

    private void ensureCanUseAiCoach(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        var activeOpt = userSubscriptionRepository.findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
                userId, SubscriptionStatus.Active, LocalDateTime.now());
        boolean canUse = activeOpt
                .map(sub -> Boolean.TRUE.equals(sub.getPlan().getCanUseAiCoach()))
                .orElse(false);
        if (!canUse) {
            throw new ForbiddenException("Bạn cần nâng cấp gói Premium để sử dụng AI Coach.");
        }
    }

    @Transactional
    public AiChatMessage saveMessage(String userId, MessageRole role, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        AiChatMessage msg = new AiChatMessage();
        msg.setUser(user);
        msg.setRole(role);
        msg.setContent(content);
        return aiChatMessageRepository.save(msg);
    }

    public List<AiChatMessageDTO> getChatHistory(String userId) {
        List<AiChatMessage> messages = aiChatMessageRepository.findByUser_IdOrderByCreatedAtAsc(userId);
        return messages.stream()
                .map(AiChatMessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public String sendMessage(String userId, String userContent) {
        ensureCanUseAiCoach(userId);

        List<AiChatMessage> recent = aiChatMessageRepository.findByUser_IdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, CONTEXT_MESSAGE_LIMIT));
        List<AiCoachRequestDTO.ChatMessageDTO> chatHistory = recent.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(m -> AiCoachRequestDTO.ChatMessageDTO.builder()
                        .role(m.getRole() == MessageRole.AI ? "assistant" : "user")
                        .content(m.getContent())
                        .build())
                .collect(Collectors.toList());

        String aiResponse = aiCoachService.getAdviceFromAiWithContext(userId, userContent, chatHistory);

        saveMessage(userId, MessageRole.USER, userContent);
        saveMessage(userId, MessageRole.AI, aiResponse);

        return aiResponse;
    }
}
