package com.nutrimate.repository;

import com.nutrimate.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {
    // Tìm gói cước đang có hiệu lực của user
    @Query("SELECT us FROM UserSubscription us WHERE us.user.id = :userId AND us.status = 'Active' AND us.endDate > CURRENT_TIMESTAMP")
    Optional<UserSubscription> findActiveSubscriptionByUserId(String userId);

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
}