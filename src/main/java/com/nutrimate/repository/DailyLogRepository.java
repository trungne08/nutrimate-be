package com.nutrimate.repository;

import com.nutrimate.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, String> {
    // Tìm log của user trong một ngày cụ thể
    Optional<DailyLog> findByUserIdAndLogDate(String userId, LocalDate logDate);
}