package com.nutrimate.repository;

import com.nutrimate.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {
    // Tìm gói cước đang có hiệu lực của user
    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.status = 'Active' AND us.endDate > CURRENT_TIMESTAMP")
    Optional<UserSubscription> findActiveSubscriptionByUserId(String userId);
}