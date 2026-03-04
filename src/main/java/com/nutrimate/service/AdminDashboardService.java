package com.nutrimate.service;

import com.nutrimate.dto.ChartDTO;
import com.nutrimate.dto.DashboardResponseDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Feedback;
import com.nutrimate.entity.User;
import com.nutrimate.repository.BookingRepository;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.repository.UserSubscriptionRepository;
import com.nutrimate.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FeedbackRepository feedbackRepository; 
    private final UserSubscriptionRepository userSubscriptionRepository;

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

    // --- 1. BIỂU ĐỒ TĂNG TRƯỞNG USER ---
    public List<ChartDTO> getUserGrowthChart(String period) {
        List<Object[]> results = "year".equalsIgnoreCase(period) 
                ? userRepository.countUsersByYear() 
                : userRepository.countUsersByMonth();

        return results.stream()
                .map(row -> new ChartDTO(row[0].toString(), ((Number) row[1]).doubleValue()))
                .collect(Collectors.toList());
    }

    // --- 2. BIỂU ĐỒ TRÒN % SUBSCRIPTION ---
    public List<ChartDTO> getSubscriptionPieChart() {
        List<Object[]> results = userSubscriptionRepository.countSubscriptionsByPlan();
        return results.stream()
                .map(row -> new ChartDTO(row[0].toString(), ((Number) row[1]).doubleValue()))
                .collect(Collectors.toList());
    }

    // --- 3. BIỂU ĐỒ DOANH THU (GỘP CẢ BOOKING + MUA GÓI) ---
    public List<ChartDTO> getRevenueGrowthChart(String period) {
        List<Object[]> bookingRev;
        List<Object[]> subRev;

        // Lấy dữ liệu từ cả 2 nguồn
        if ("year".equalsIgnoreCase(period)) {
            bookingRev = bookingRepository.getRevenueByYear();
            subRev = userSubscriptionRepository.getRevenueByYear();
        } else if ("week".equalsIgnoreCase(period)) {
            bookingRev = bookingRepository.getRevenueByWeek();
            subRev = userSubscriptionRepository.getRevenueByWeek();
        } else {
            bookingRev = bookingRepository.getRevenueByMonth();
            subRev = userSubscriptionRepository.getRevenueByMonth();
        }

        // Dùng TreeMap để gộp tiền lại và tự động sắp xếp theo thứ tự thời gian tăng dần
        Map<String, Double> mergedRevenue = new java.util.TreeMap<>();

        for (Object[] row : bookingRev) {
            String label = row[0].toString();
            Double amount = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            mergedRevenue.put(label, mergedRevenue.getOrDefault(label, 0.0) + amount);
        }

        for (Object[] row : subRev) {
            String label = row[0].toString();
            Double amount = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            mergedRevenue.put(label, mergedRevenue.getOrDefault(label, 0.0) + amount);
        }

        return mergedRevenue.entrySet().stream()
                .map(entry -> new ChartDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}