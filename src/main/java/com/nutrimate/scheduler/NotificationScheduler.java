package com.nutrimate.scheduler;

import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Notification.NotificationType;
import com.nutrimate.entity.UserChallenge;
import com.nutrimate.repository.BookingRepository;
import com.nutrimate.repository.UserChallengeRepository;
import com.nutrimate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final BookingRepository bookingRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */15 * * * *") // mỗi 15 phút
    @Transactional
    public void remindUpcomingAppointments() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.plusHours(1);
        LocalDateTime end = now.plusHours(2);
        List<Booking> bookings = bookingRepository.findUpcomingToRemind(start, end);
        for (Booking b : bookings) {
            if (b.getMember() == null || b.getMember().getId() == null) continue;
            String userId = b.getMember().getId();
            String title = "Nhắc lịch hẹn sắp tới";
            String msg = String.format("Bạn có lịch hẹn với chuyên gia vào lúc %s. Hãy chuẩn bị nhé!",
                    b.getBookingTime() != null ? b.getBookingTime().toLocalTime().toString() : "");
            notificationService.createAndPush(userId, title, msg, NotificationType.APPOINTMENT);
            b.setIsReminded(true);
            bookingRepository.save(b);
            log.debug("Reminded user {} for booking {}", userId, b.getId());
        }
    }

    @Scheduled(cron = "0 0 20 * * *") // 20:00 mỗi ngày
    @Transactional
    public void remindDailyChallengeCheckin() {
        LocalDate today = LocalDate.now();
        List<UserChallenge> active = userChallengeRepository.findByStatus(UserChallenge.ChallengeStatus.IN_PROGRESS);
        for (UserChallenge uc : active) {
            if (uc.getLastCheckInDate() != null && uc.getLastCheckInDate().equals(today)) continue;
            if (uc.getUser() == null || uc.getUser().getId() == null) continue;
            String userId = uc.getUser().getId();
            String challengeTitle = uc.getChallenge() != null ? uc.getChallenge().getTitle() : "Thử thách";
            String title = "Nhắc nhở check-in thử thách";
            String msg = String.format("Bạn chưa check-in hôm nay cho thử thách \"%s\". Đừng bỏ lỡ nhé!", challengeTitle);
            notificationService.createAndPush(userId, title, msg, NotificationType.CHALLENGE);
            log.debug("Reminded user {} for challenge check-in", userId);
        }
    }
}
