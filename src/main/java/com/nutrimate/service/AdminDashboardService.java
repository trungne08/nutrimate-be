package com.nutrimate.service;

import com.nutrimate.dto.DashboardResponseDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Feedback;
import com.nutrimate.entity.User;
import com.nutrimate.repository.BookingRepository;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FeedbackRepository feedbackRepository; 

    public DashboardResponseDTO getDashboardStats() {
        BigDecimal totalRevenue = bookingRepository.calculateTotalRevenue();
        
        return DashboardResponseDTO.builder()
                .totalUsers(userRepository.count())
                .totalTransactions(bookingRepository.count())
                .totalFeedbacks(feedbackRepository.count()) // 👇 Đã dùng số liệu THẬT
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .build();
    }
    
    public Page<User> getPaginatedUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable);
    }

    // Lấy danh sách Giao dịch (Phân trang)
    public Page<Booking> getPaginatedTransactions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("bookingTime").descending());
        return bookingRepository.findAll(pageable);
    }

    public Page<Feedback> getPaginatedFeedbacks(int page, int size) {
        // Sắp xếp feedback mới nhất lên đầu
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return feedbackRepository.findAll(pageable);
    }
}