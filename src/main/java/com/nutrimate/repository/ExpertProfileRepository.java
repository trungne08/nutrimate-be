package com.nutrimate.repository;

import com.nutrimate.entity.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, String> {
    Optional<ExpertProfile> findByUserId(String userId);
    
    // Tìm chuyên gia theo chuyên môn (VD: PT, Nutritionist)
    List<ExpertProfile> findBySpecializationContainingIgnoreCase(String specialization);
    
    // Tìm các chuyên gia có đánh giá cao
    List<ExpertProfile> findByRatingGreaterThanEqual(Float rating);

    @Query("SELECT e FROM ExpertProfile e WHERE " +
           "(:minRating IS NULL OR e.rating >= :minRating) AND " +
           "(:maxPrice IS NULL OR e.hourlyRate <= :maxPrice)")
    List<ExpertProfile> searchExperts(@Param("minRating") Float minRating, 
                                      @Param("maxPrice") BigDecimal maxPrice);
}