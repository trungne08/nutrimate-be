package com.nutrimate.repository;

import com.nutrimate.entity.HealthMetricHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HealthMetricHistoryRepository extends JpaRepository<HealthMetricHistory, String> {
    // Lấy lịch sử để vẽ biểu đồ (sắp xếp theo ngày ghi nhận)
    List<HealthMetricHistory> findByUserIdOrderByRecordedAtAsc(String userId);
}