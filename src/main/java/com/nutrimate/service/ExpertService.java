package com.nutrimate.service;

import com.nutrimate.entity.ExpertProfile;
import com.nutrimate.repository.ExpertProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertService {
    private final ExpertProfileRepository expertRepository;

    // 5.1 Search
    public List<ExpertProfile> searchExperts(Float minRating, BigDecimal maxPrice) {
        return expertRepository.searchExperts(minRating, maxPrice);
    }

    // 5.2 Get Detail
    public ExpertProfile getExpertById(String expertId) {
        return expertRepository.findById(expertId)
                .orElseThrow(() -> new RuntimeException("Expert not found"));
    }
}