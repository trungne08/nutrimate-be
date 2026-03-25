package com.nutrimate.repository;

import com.nutrimate.entity.UserSubscription;
import com.nutrimate.entity.UserSubscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {
    // Tìm 1 gói đang Active gần hết hạn nhất (endDate xa nhất nhưng vẫn còn hiệu lực) cho 1 user
    Optional<UserSubscription> findFirstByUser_IdAndStatusAndEndDateAfterOrderByEndDateDesc(
            String userId,
            SubscriptionStatus status,
            LocalDateTime now
    );

    // 1. Dùng cho Biểu đồ tròn: Đếm số lượng từng gói
    @Query("SELECT s.plan.planName, COUNT(s) FROM UserSubscription s GROUP BY s.plan.planName")
    List<Object[]> countSubscriptionsByPlan();

    // 2. Dùng cho Biểu đồ doanh thu: Gom tiền mua gói theo Tuần/Tháng/Năm
    @Query(value = "SELECT DATE_FORMAT(s.start_date, '%Y-%m') as label, SUM(p.price) FROM User_Subscriptions s JOIN `Subscription_Plans` p ON s.plan_id = p.plan_id WHERE s.status = 'ACTIVE' GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getRevenueByMonth();

    @Query(value = "SELECT YEAR(s.start_date) as label, SUM(p.price) FROM User_Subscriptions s JOIN `Subscription_Plans` p ON s.plan_id = p.plan_id WHERE s.status = 'ACTIVE' GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getRevenueByYear();

    @Query(value = "SELECT DATE_FORMAT(s.start_date, '%x-W%v') as label, SUM(p.price) FROM User_Subscriptions s JOIN `Subscription_Plans` p ON s.plan_id = p.plan_id WHERE s.status = 'ACTIVE' GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> getRevenueByWeek();

    @Query("SELECT SUM(s.plan.price) FROM UserSubscription s WHERE s.status = 'Active'")
    java.math.BigDecimal calculateTotalRevenue();

    @Query("SELECT COUNT(s) FROM UserSubscription s WHERE s.status = 'Active' AND s.orderCode IS NOT NULL")
    long countPaidActiveSubscriptions();
}