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

    // 8.1 Xem danh sách thử thách (Public)
    public List<Challenge> getAllChallenges() {
        return challengeRepository.findAll();
    }

    // 8.2 [ADMIN] Tạo thử thách
    @Transactional
    public Challenge createChallenge(ChallengeDTO.CreateRequest req) throws IOException {
        Challenge challenge = new Challenge();
        challenge.setTitle(req.getTitle());
        challenge.setDescription(req.getDescription());
        challenge.setDurationDays(req.getDurationDays());
        challenge.setLevel(req.getLevel() != null ? req.getLevel() : Challenge.ChallengeLevel.EASY);
        setChallengeImage(challenge, req);
        return challengeRepository.save(challenge);
    }

    @Transactional
    public Challenge updateChallenge(String id, ChallengeDTO.CreateRequest req) throws IOException {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));
        
        challenge.setTitle(req.getTitle());
        challenge.setDescription(req.getDescription());
        challenge.setDurationDays(req.getDurationDays());
        if (req.getLevel() != null) challenge.setLevel(req.getLevel());
        setChallengeImage(challenge, req);
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
    public UserChallenge joinChallenge(String userId, String challengeId) {
        if (userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent()) {
            throw new BadRequestException("You have already joined this challenge!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

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
            dto.setImageUrl(c.getImageUrl());

            // Info cá nhân
            dto.setJoined(true);
            dto.setDaysCompleted(uc.getDaysCompleted());
            dto.setStatus(uc.getStatus().name());
            
            return dto;
        }).collect(Collectors.toList());
    }
}