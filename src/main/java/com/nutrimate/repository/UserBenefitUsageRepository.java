package com.nutrimate.repository;

import com.nutrimate.entity.UserBenefitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserBenefitUsageRepository extends JpaRepository<UserBenefitUsage, String> {
    // Tìm thông tin sử dụng quyền lợi theo subscription ID
    Optional<UserBenefitUsage> findBySubscriptionId(String subscriptionId);

    Optional<UserBenefitUsage> findByUserId(String userId);
}