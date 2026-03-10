package com.nutrimate.service;

import com.nutrimate.entity.Notification;
import com.nutrimate.entity.Notification.NotificationType;
import com.nutrimate.entity.User;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.NotificationRepository;
import com.nutrimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createAndPush(String userId, String title, String message, NotificationType type) {
        return createAndPush(userId, title, message, type, null);
    }

    @Transactional
    public Notification createAndPush(String userId, String title, String message, NotificationType type,
                                     java.util.Map<String, Object> extraPayload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setIsRead(false);
        Notification saved = notificationRepository.save(n);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", saved.getId());
        payload.put("title", saved.getTitle());
        payload.put("message", saved.getMessage());
        payload.put("type", saved.getType().name());
        payload.put("isRead", saved.getIsRead());
        payload.put("createdAt", saved.getCreatedAt());
        if (extraPayload != null) payload.putAll(extraPayload);

        try {
            messagingTemplate.convertAndSend("/queue/notifications/" + userId, (Object) payload);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to user {}: {}", userId, e.getMessage());
        }
        return saved;
    }

    public Page<Notification> getNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void markAsRead(String userId, String notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
