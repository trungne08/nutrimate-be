package com.nutrimate.repository;

import com.nutrimate.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, String> {
    // Lấy tất cả món ăn trong một Daily Log
    List<MealLog> findByDailyLogId(String dailyLogId);
}