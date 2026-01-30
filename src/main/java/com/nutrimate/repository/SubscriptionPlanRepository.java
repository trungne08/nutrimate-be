package com.nutrimate.repository;

import com.nutrimate.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> { 
    
    List<SubscriptionPlan> findByIsActiveTrue();

    Optional<SubscriptionPlan> findFirstByPrice(BigDecimal price);
}