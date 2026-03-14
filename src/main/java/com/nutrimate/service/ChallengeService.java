package com.nutrimate.service;

import com.nutrimate.dto.ChallengeDTO;
import com.nutrimate.entity.*;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final CheckInLogRepository checkInLogRepository;

    // 8.1 Xem danh sách thử thách (Public)
    public List<Challenge> getAllChallenges() {
        return challengeRepository.findAll();
    }

    // 8.2 [ADMIN] Tạo thử thách
    @Transactional
    public Challenge createChallenge(ChallengeDTO.CreateRequest request) {
        Challenge challenge = new Challenge();
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setDurationDays(request.getDurationDays());

        // Map Level (Enum)
        if (request.getLevel() != null) {
            challenge.setLevel(request.getLevel());
        }
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            try {
                String uploadedUrl = fileUploadService.uploadFile(request.getImageFile());
                challenge.setImageUrl(uploadedUrl);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
            }
        } else if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            challenge.setImageUrl(request.getImageUrl());
        }

        return challengeRepository.save(challenge);
    }

    @Transactional
    public Challenge updateChallenge(String id, ChallengeDTO.CreateRequest request) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thử thách với ID: " + id));
        challenge.setTitle(request.getTitle());
        if (request.getDescription() != null)
            challenge.setDescription(request.getDescription());
        if (request.getDurationDays() != null)
            challenge.setDurationDays(request.getDurationDays());
        if (request.getLevel() != null)
            challenge.setLevel(request.getLevel());
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            try {
                String uploadedUrl = fileUploadService.uploadFile(request.getImageFile());
                challenge.setImageUrl(uploadedUrl);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh khi update: " + e.getMessage());
            }
        } else if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            challenge.setImageUrl(request.getImageUrl());
        }

        return challengeRepository.save(challenge);
    }

    private void setChallengeImage(Challenge challenge, ChallengeDTO.CreateRequest req) throws IOException {
        if (req.getImageFile() != null && !req.getImageFile().isEmpty()) {
            String url = fileUploadService.uploadFile(req.getImageFile());
            challenge.setImageUrl(url);
        } else if (req.getImageUrl() != null && !req.getImageUrl().trim().isEmpty()) {
            challenge.setImageUrl(req.getImageUrl().trim());
        }
    }

    @Transactional
    public void deleteChallenge(String id) {
        if (!challengeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Challenge not found");
        }
        challengeRepository.deleteById(id);
    }

    @Transactional
    public void joinChallenge(String userId, String challengeId) {
        var existing = userChallengeRepository.findByUserIdAndChallengeIdAndStatus(
                userId, challengeId, UserChallenge.ChallengeStatus.IN_PROGRESS);

        if (existing.isPresent()) {
            throw new RuntimeException("You have already joined this challenge!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        UserChallenge uc = new UserChallenge();
        uc.setUser(user);
        uc.setChallenge(challenge);
        uc.setJoinDate(LocalDate.now());
        uc.setDaysCompleted(0);
        uc.setStatus(UserChallenge.ChallengeStatus.IN_PROGRESS);

        userChallengeRepository.save(uc);
    }

    @Transactional
    public void checkInChallenge(String userId, String challengeId) {
        // 1. Tìm UserChallenge
        UserChallenge uc = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa tham gia thử thách này"));

        // 2. Kiểm tra trạng thái
        if (uc.getStatus() != UserChallenge.ChallengeStatus.IN_PROGRESS) {
            throw new BadRequestException("Thử thách này đã kết thúc hoặc đã hoàn thành.");
        }

        // 3. Chốt chặn điểm danh 1 lần/ngày
        LocalDate today = LocalDate.now();
        if (uc.getLastCheckInDate() != null && uc.getLastCheckInDate().isEqual(today)) {
            throw new BadRequestException("Hôm nay bác đã điểm danh rồi, mai quay lại nhé!");
        }

        // 4. Logic tăng số ngày & Lưu lịch sử
        int totalDays = uc.getChallenge().getDurationDays();

        if (uc.getDaysCompleted() < totalDays) {
            uc.setDaysCompleted(uc.getDaysCompleted() + 1);
            uc.setLastCheckInDate(today); // Sử dụng biến today đã khai báo ở trên
            
            // Lưu log điểm danh chi tiết
            CheckInLog log = new CheckInLog();
            log.setUserChallenge(uc);
            log.setCheckinDate(today); // Đồng bộ sử dụng biến today
            checkInLogRepository.save(log);
        }

        // 5. Kiểm tra về đích
        if (uc.getDaysCompleted() >= totalDays) {
            uc.setStatus(UserChallenge.ChallengeStatus.COMPLETED);
        }

        userChallengeRepository.save(uc);
    }

    // 8.6 [MEMBER] Xem thử thách của tôi (Kèm tiến độ)
    public List<ChallengeDTO.Response> getMyChallenges(String userId) {

        List<UserChallenge> myChallenges = userChallengeRepository.findByUserId(userId);

        return myChallenges.stream().map(uc -> {
            ChallengeDTO.Response dto = new ChallengeDTO.Response();
            Challenge c = uc.getChallenge();

            // Mapping thông tin chung
            dto.setId(c.getId());
            dto.setTitle(c.getTitle());
            dto.setDescription(c.getDescription());
            dto.setDurationDays(c.getDurationDays());
            dto.setLevel(c.getLevel().name());

            // 👇 QUAN TRỌNG: Map ảnh từ Entity sang DTO
            // (Đảm bảo Entity Challenge có getter getImage() hoặc getImageUrl())
            dto.setImageUrl(c.getImageUrl());

            // Mapping thông tin cá nhân
            dto.setJoined(true);
            dto.setDaysCompleted(uc.getDaysCompleted());
            dto.setStatus(uc.getStatus().name());

            // Tính phần trăm
            if (c.getDurationDays() != null && c.getDurationDays() > 0) {
                int percent = (int) ((double) uc.getDaysCompleted() / c.getDurationDays() * 100);
                dto.setProgressPercent(Math.min(percent, 100));
            } else {
                dto.setProgressPercent(0);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    public List<LocalDate> getCheckInHistory(String userId, String challengeId) {
        UserChallenge uc = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu thử thách"));

        return checkInLogRepository.findByUserChallengeIdOrderByCheckinDateDesc(uc.getId())
                .stream()
                .map(CheckInLog::getCheckinDate)
                .collect(Collectors.toList());
    }
}
