package com.nutrimate.repository;

import com.nutrimate.entity.SystemFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemFeedbackRepository extends JpaRepository<SystemFeedback, String> {
    
    Page<SystemFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT AVG(s.rating) FROM SystemFeedback s")
    Double getAverageSystemRating();
}