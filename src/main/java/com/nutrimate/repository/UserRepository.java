package com.nutrimate.repository;

import com.nutrimate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByEmail(String email);

    Optional<User> findByCognitoId(String cognitoId);
    Page<User> findByRole(User.UserRole role, Pageable pageable);

    // Đếm User theo tháng (Định dạng: 2026-01, 2026-02)
    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') as label, COUNT(user_id) FROM Users GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> countUsersByMonth();

    // Đếm User theo năm (Định dạng: 2025, 2026)
    @Query(value = "SELECT YEAR(created_at) as label, COUNT(user_id) FROM Users GROUP BY label ORDER BY label", nativeQuery = true)
    List<Object[]> countUsersByYear();
}
