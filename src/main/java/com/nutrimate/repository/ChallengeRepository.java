package com.nutrimate.repository;

import com.nutrimate.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, String> {
}