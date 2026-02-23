package com.nutrimate.repository;

import com.nutrimate.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, String> {
    // JpaRepository đã tự động có sẵn:
    // 1. long count(); -> Để đếm tổng số feedback cho Dashboard
    // 2. Page<Feedback> findAll(Pageable pageable); -> Để lấy danh sách phân trang
boolean existsByBookingId(String bookingId);
}