package com.nutrimate.scheduler;

import com.nutrimate.entity.UserChallenge;
import com.nutrimate.repository.UserChallengeRepository;
import com.nutrimate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeReminderScheduler {

    private final UserChallengeRepository userChallengeRepository;
    private final NotificationService notificationService;

    /**
     * Job buổi sáng 7:00 - nhắc nhở thử thách đang diễn ra cho từng user.
     */
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void morningChallengeReminder() {
        sendGroupedChallengeReminders();
    }

    /**
     * Job buổi tối 19:00 - nhắc nhở thử thách đang diễn ra cho từng user.
     */
    @Scheduled(cron = "0 0 19 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void eveningChallengeReminder() {
        sendGroupedChallengeReminders();
    }

    /**
     * Logic gom nhóm: lấy tất cả thử thách IN_PROGRESS, group theo user và gửi 1 reminder cho mỗi user.
     */
    private void sendGroupedChallengeReminders() {
        // Lấy tất cả thử thách đang IN_PROGRESS
        List<UserChallenge> active = userChallengeRepository.findByStatus(UserChallenge.ChallengeStatus.IN_PROGRESS);

        if (active.isEmpty()) {
            log.debug("ChallengeReminderScheduler: no active challenges to remind.");
            return;
        }

        // Group by User: một user nhận đúng 1 notification với danh sách challenge
        Map<com.nutrimate.entity.User, List<UserChallenge>> groupedByUser = active.stream()
                .filter(uc -> uc.getUser() != null && uc.getUser().getId() != null)
                .collect(Collectors.groupingBy(UserChallenge::getUser));

        groupedByUser.forEach((user, challenges) -> {
            List<String> challengeNames = challenges.stream()
                    .map(uc -> uc.getChallenge() != null ? uc.getChallenge().getTitle() : "Thử thách")
                    .distinct()
                    .toList();

            if (challengeNames.isEmpty()) {
                return;
            }

            notificationService.sendChallengeReminder(user, challengeNames);
            log.debug("ChallengeReminderScheduler: sent reminder to user {} for challenges: {}",
                    user.getId(), String.join(", ", challengeNames));
        });
    }
}

