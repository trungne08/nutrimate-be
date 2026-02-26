package com.nutrimate.repository;

import com.nutrimate.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, String> {
boolean existsByBookingId(String bookingId);

@Query("SELECT f FROM Feedback f WHERE f.expert.id = :expertId ORDER BY f.createdAt DESC")
    Page<Feedback> findByExpertId(@Param("expertId") String expertId, Pageable pageable);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.expert.id = :expertId")
    Double getAverageRatingByExpertId(@Param("expertId") String expertId);
}