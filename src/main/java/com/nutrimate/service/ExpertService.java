package com.nutrimate.service;

import com.nutrimate.entity.ExpertProfile;
import com.nutrimate.entity.ExpertProfile.ApprovalStatus;
import com.nutrimate.entity.User;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.ExpertProfileRepository;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.dto.ExpertApplicationDTO;
import com.nutrimate.exception.BadRequestException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpertService {
    private final ExpertProfileRepository expertRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    // 5.1 Search
    public List<ExpertProfile> searchExperts(Float minRating, BigDecimal maxPrice) {
        // 1. Chỉ lấy những Expert đã được DUYỆT (APPROVED)
        // (Thay vì dùng findAll() như cũ)
        List<ExpertProfile> experts = expertRepository.findByStatus(ExpertProfile.ApprovalStatus.APPROVED);

        // 2. Lọc tiếp theo Rating và Price (nếu user có truyền vào)
        return experts.stream()
                .filter(e -> minRating == null || (e.getRating() != null && e.getRating() >= minRating))
                .filter(e -> maxPrice == null || (e.getHourlyRate() != null && e.getHourlyRate().compareTo(maxPrice) <= 0))
                .toList();
    }

    // 5.2 Get Detail
    public ExpertProfile getExpertById(String expertId) {
        return expertRepository.findById(expertId)
                .orElseThrow(() -> new RuntimeException("Expert not found"));
    }

    @Transactional
    public ExpertProfile submitApplication(String userId, ExpertApplicationDTO req, MultipartFile certificateFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Check xem đã có hồ sơ chưa
        Optional<ExpertProfile> existingProfile = expertRepository.findByUserId(userId);
        
        if (existingProfile.isPresent()) {
            ExpertProfile profile = existingProfile.get();
            // Nếu đang chờ duyệt -> Báo lỗi
            if (profile.getStatus() == ApprovalStatus.PENDING) {
                throw new BadRequestException("Bạn đã gửi đơn rồi, vui lòng chờ Admin duyệt.");
            }
            // Nếu đã là Expert -> Báo lỗi
            if (profile.getStatus() == ApprovalStatus.APPROVED) {
                throw new BadRequestException("Bạn đã là Expert rồi!");
            }
            
            // Nếu bị từ chối (REJECTED) -> Cho phép update lại để nộp lại
            updateProfileData(profile, req, certificateFile);
            profile.setStatus(ApprovalStatus.PENDING); // Reset về chờ duyệt
            return expertRepository.save(profile);
        }

        // 2. Tạo mới hoàn toàn
        ExpertProfile newProfile = new ExpertProfile();
        newProfile.setUser(user);
        newProfile.setRating(0.0f);
        newProfile.setStatus(ApprovalStatus.PENDING); // Set trạng thái chờ
        
        updateProfileData(newProfile, req, certificateFile);

        return expertRepository.save(newProfile);
    }

    private void updateProfileData(ExpertProfile profile, ExpertApplicationDTO req, MultipartFile file) {
        profile.setSpecialization(req.getSpecialization());
        profile.setBio(req.getBio());
        profile.setYearsExperience(req.getYearsExperience());
        profile.setHourlyRate(req.getHourlyRate());

        if (file != null && !file.isEmpty()) {
            try {
                String url = fileUploadService.uploadFile(file);
                profile.setCertification(url); // 👈 Khớp với tên biến trong Entity của bạn
            } catch (IOException e) {
                throw new BadRequestException("Lỗi upload chứng chỉ: " + e.getMessage());
            }
        }
    }
}