package com.nutrimate.service;

import com.nutrimate.dto.AiChatMessageDTO;
import com.nutrimate.dto.AiChatQuotaDTO;
import com.nutrimate.dto.AiChatSendResponseDTO;
import com.nutrimate.dto.AiCoachRequestDTO;
import com.nutrimate.entity.AiChatMessage;
import com.nutrimate.entity.AiChatMessage.MessageRole;
import com.nutrimate.entity.SubscriptionPlan;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private static final int CONTEXT_MESSAGE_LIMIT = 10;
    private static final int LIMIT_FREE = 5;
    private static final int LIMIT_BASIC = 10;
    private static final int LIMIT_UNLIMITED = -1;

    private final AiChatMessageRepository aiChatMessageRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final AiCoachService aiCoachService;

    private LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    private int getDailyLimit(String userId) {
        var activeOpt = userSubscriptionRepository.findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
                userId, SubscriptionStatus.Active, LocalDateTime.now());
        if (activeOpt.isEmpty()) return LIMIT_FREE;
        SubscriptionPlan plan = activeOpt.get().getPlan();
        if (plan == null) return LIMIT_FREE;
        String name = (plan.getPlanName() != null ? plan.getPlanName() : "").toUpperCase();
        if (name.contains("PREMIUM") || name.contains("EXPERT")) return LIMIT_UNLIMITED;
        if (name.contains("BASIC") || (plan.getPrice() != null && plan.getPrice().compareTo(BigDecimal.ZERO) > 0)) {
            return LIMIT_BASIC;
        }
        return LIMIT_FREE;
    }

    private void checkDailyQuota(String userId) {
        int limit = getDailyLimit(userId);
        if (limit == LIMIT_UNLIMITED) return;
        LocalDateTime startOfDay = startOfToday();
        long used = aiChatMessageRepository.countByUser_IdAndRoleAndCreatedAtGreaterThanEqual(
                userId, MessageRole.USER, startOfDay);
        if (used >= limit) {
            throw new ForbiddenException("Bạn đã hết lượt trò chuyện với AI hôm nay. Vui lòng nâng cấp gói để tiếp tục!");
        }
    }

    public AiChatQuotaDTO getQuota(String userId) {
        userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        int limit = getDailyLimit(userId);
        LocalDateTime startOfDay = startOfToday();
        long used = aiChatMessageRepository.countByUser_IdAndRoleAndCreatedAtGreaterThanEqual(
                userId, MessageRole.USER, startOfDay);
        int remaining = (limit == LIMIT_UNLIMITED) ? Integer.MAX_VALUE : (int) Math.max(0, limit - used);
        return AiChatQuotaDTO.builder()
                .dailyLimit(limit == LIMIT_UNLIMITED ? -1 : limit)
                .usedToday((int) used)
                .remainingChats(limit == LIMIT_UNLIMITED ? -1 : remaining)
                .build();
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
    public AiChatSendResponseDTO sendMessage(String userId, String userContent) {
        checkDailyQuota(userId);

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

        AiChatQuotaDTO quota = getQuota(userId);
        return AiChatSendResponseDTO.builder()
                .response(aiResponse)
                .remainingChats(quota.getRemainingChats())
                .dailyLimit(quota.getDailyLimit())
                .build();
    }
}
