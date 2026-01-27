package com.nutrimate.service;

import com.nutrimate.dto.ChallengeDTO;
import com.nutrimate.entity.*;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;

    // 8.1 Xem danh sách thử thách (Public)
    public List<Challenge> getAllChallenges() {
        return challengeRepository.findAll();
    }

    // 8.2 [ADMIN] Tạo thử thách
    @Transactional
    public Challenge createChallenge(ChallengeDTO.CreateRequest req) {
        Challenge challenge = new Challenge();
        challenge.setTitle(req.getTitle());
        challenge.setDescription(req.getDescription());
        challenge.setDurationDays(req.getDurationDays());
        challenge.setLevel(req.getLevel() != null ? req.getLevel() : Challenge.ChallengeLevel.EASY);
        return challengeRepository.save(challenge);
    }

    // 8.3 [ADMIN] Sửa thử thách
    @Transactional
    public Challenge updateChallenge(String id, ChallengeDTO.CreateRequest req) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        
        challenge.setTitle(req.getTitle());
        challenge.setDescription(req.getDescription());
        challenge.setDurationDays(req.getDurationDays());
        if (req.getLevel() != null) challenge.setLevel(req.getLevel());
        
        return challengeRepository.save(challenge);
    }

    // 8.4 [ADMIN] Xóa thử thách
    @Transactional
    public void deleteChallenge(String id) {
        if (!challengeRepository.existsById(id)) {
            throw new RuntimeException("Challenge not found");
        }
        // Lưu ý: Nếu đã có người tham gia thì nên dùng Soft Delete hoặc chặn xóa
        // Ở đây mình xóa thẳng để đơn giản code demo
        challengeRepository.deleteById(id);
    }

    // 8.5 [MEMBER] Tham gia thử thách
    @Transactional
    public UserChallenge joinChallenge(String userId, String challengeId) {
        // 1. Check xem đã tham gia chưa
        if (userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent()) {
            throw new RuntimeException("Bạn đã tham gia thử thách này rồi!");
        }

        User user = userRepository.findById(userId).orElseThrow();
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        // 2. Tạo record UserChallenge
        UserChallenge uc = new UserChallenge();
        uc.setUser(user);
        uc.setChallenge(challenge);
        uc.setJoinDate(LocalDate.now());
        uc.setStatus(UserChallenge.ChallengeStatus.IN_PROGRESS);
        uc.setDaysCompleted(0);

        return userChallengeRepository.save(uc);
    }

    // 8.6 [MEMBER] Xem thử thách của tôi (Kèm tiến độ)
    public List<ChallengeDTO.Response> getMyChallenges(String userId) {
        List<UserChallenge> myChallenges = userChallengeRepository.findByUserId(userId);

        return myChallenges.stream().map(uc -> {
            ChallengeDTO.Response dto = new ChallengeDTO.Response();
            Challenge c = uc.getChallenge();
            
            // Info thử thách
            dto.setId(c.getId());
            dto.setTitle(c.getTitle());
            dto.setDescription(c.getDescription());
            dto.setDurationDays(c.getDurationDays());
            dto.setLevel(c.getLevel().name());

            // Info cá nhân
            dto.setJoined(true);
            dto.setDaysCompleted(uc.getDaysCompleted());
            dto.setStatus(uc.getStatus().name());
            
            return dto;
        }).collect(Collectors.toList());
    }
}