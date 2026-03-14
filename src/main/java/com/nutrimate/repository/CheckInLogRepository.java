package com.nutrimate.repository;

import com.nutrimate.entity.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CheckInLogRepository extends JpaRepository<CheckInLog, String> {
    List<CheckInLog> findByUserChallengeIdOrderByCheckinDateDesc(String userChallengeId);
}
