package com.nutrimate.repository;

import com.nutrimate.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, String> {
    // Lấy danh sách thử thách user đang tham gia
    List<UserChallenge> findByUserId(String userId);

    Optional<UserChallenge> findByUserIdAndChallengeId(String userId, String challengeId);
}