package com.nutrimate.service;

import com.nutrimate.entity.Notification;
import com.nutrimate.entity.Notification.NotificationType;
import com.nutrimate.entity.User;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.NotificationRepository;
import com.nutrimate.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

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

    /**
     * Gửi nhắc nhở thử thách cho user: lưu Notification, bắn WebSocket và gửi Email HTML bất đồng bộ.
     */
    @Transactional
    public void sendChallengeReminder(User user, List<String> challengeNames) {
        if (user == null || user.getId() == null || user.getEmail() == null) {
            log.warn("Skip challenge reminder because user or email is null");
            return;
        }

        String joinedNames = String.join(", ", challengeNames);

        String title = "Nutrimate nhắc nhở thử thách!";
        String message = "Hôm nay bạn có các thử thách đang chờ hoàn thành: " + joinedNames;

        // Lưu notification + bắn WebSocket
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(NotificationType.CHALLENGE);
        n.setIsRead(false);
        Notification saved = notificationRepository.save(n);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", saved.getId());
        payload.put("title", saved.getTitle());
        payload.put("message", saved.getMessage());
        payload.put("type", saved.getType().name());
        payload.put("isRead", saved.getIsRead());
        payload.put("createdAt", saved.getCreatedAt());

        try {
            messagingTemplate.convertAndSend("/queue/notifications/" + user.getId(), (Object) payload);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket challenge notification to user {}: {}", user.getId(), e.getMessage());
        }

        // Gửi email bất đồng bộ
        try {
            sendChallengeReminderEmail(user, challengeNames);
        } catch (Exception e) {
            // Không được để exception làm chết scheduler, chỉ log warning
            log.warn("Failed to schedule async email for challenge reminder to user {}: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * Gửi email HTML nhắc nhở thử thách - chạy bất đồng bộ để không block scheduler.
     */
    @Async
    public void sendChallengeReminderEmail(User user, List<String> challengeNames) {
        if (user == null || user.getEmail() == null) {
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setTo(user.getEmail());
            helper.setSubject("Nutrimate - Nhắc nhở thử thách hôm nay");

            String displayName = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : (user.getUsername() != null && !user.getUsername().isBlank()
                    ? user.getUsername()
                    : user.getEmail());

            StringBuilder listHtml = new StringBuilder();
            listHtml.append("<ul style=\"padding-left:20px; margin: 8px 0 16px 0;\">");
            for (String name : challengeNames) {
                listHtml.append("<li style=\"margin-bottom:4px; font-size:14px; color:#155724;\">")
                        .append(escapeHtml(name))
                        .append("</li>");
            }
            listHtml.append("</ul>");

            String html = """
                    <!DOCTYPE html>
                    <html lang="vi">
                    <head>
                        <meta charset="UTF-8" />
                        <title>Nutrimate - Nhắc nhở thử thách</title>
                    </head>
                    <body style="margin:0;padding:0;background-color:#f5f9f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f5f9f5;padding:24px 0;">
                            <tr>
                                <td align="center">
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background-color:#ffffff;border-radius:16px;box-shadow:0 8px 24px rgba(0,0,0,0.06);overflow:hidden;">
                                        <tr>
                                            <td style="background:linear-gradient(135deg,#2ecc71,#27ae60);padding:20px 24px;">
                                                <h1 style="margin:0;font-size:20px;color:#ffffff;font-weight:700;letter-spacing:0.02em;">Nutrimate Challenge</h1>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="padding:24px 24px 8px 24px;">
                                                <p style="margin:0 0 12px 0;font-size:15px;color:#14532d;font-weight:600;">
                                                    Chào %s,
                                                </p>
                                                <p style="margin:0 0 12px 0;font-size:14px;color:#166534;line-height:1.6;">
                                                    Hôm nay bạn có các thử thách đang chờ hoàn thành:
                                                </p>
                                                %s
                                                <p style="margin:8px 0 20px 0;font-size:13px;color:#4b5563;line-height:1.6;">
                                                    Duy trì thói quen mỗi ngày sẽ giúp bạn tiến gần hơn tới mục tiêu sức khỏe của mình. Nutrimate sẽ đồng hành cùng bạn trong từng bước nhỏ.
                                                </p>
                                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:0 0 8px 0;">
                                                    <tr>
                                                        <td align="center" bgcolor="#22c55e" style="border-radius:999px;">
                                                            <a href="https://www.nutrimate.site" target="_blank"
                                                               style="display:inline-block;padding:10px 26px;font-size:14px;color:#ffffff;text-decoration:none;font-weight:600;letter-spacing:0.04em;text-transform:uppercase;">
                                                                Vào app ngay
                                                            </a>
                                                        </td>
                                                    </tr>
                                                </table>
                                                <p style="margin:12px 0 0 0;font-size:12px;color:#6b7280;">
                                                    Nếu nút không hoạt động, hãy truy cập: <a href="https://www.nutrimate.site" style="color:#16a34a;text-decoration:underline;">https://www.nutrimate.site</a>
                                                </p>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="padding:16px 24px 20px 24px;border-top:1px solid #e5e7eb;">
                                                <p style="margin:0;font-size:11px;color:#9ca3af;line-height:1.5;">
                                                    Bạn nhận được email này vì đã tham gia thử thách trên Nutrimate.
                                                    Nếu đây không phải là bạn, vui lòng bỏ qua email.
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """.formatted(escapeHtml(displayName), listHtml.toString());

            helper.setText(html, true);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            // Chỉ log, không ném lỗi để tránh kill thread scheduler
            log.warn("Failed to send challenge reminder email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
