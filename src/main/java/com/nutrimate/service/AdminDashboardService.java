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
import com.nutrimate.repository.SystemFeedbackRepository;
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
    private final SystemFeedbackRepository systemFeedbackRepository;

    public DashboardResponseDTO getDashboardStats() {
        // 1. Tính tổng doanh thu từ 2 nguồn (Giữ nguyên như cũ)
        BigDecimal bookingRevenue = bookingRepository.calculateTotalRevenue();
        BigDecimal subRevenue = userSubscriptionRepository.calculateTotalRevenue();
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        if (bookingRevenue != null) {
            totalRevenue = totalRevenue.add(bookingRevenue);
        }
        if (subRevenue != null) {
            totalRevenue = totalRevenue.add(subRevenue);
        }
        
        // 2. Tính tổng số lượng giao dịch THÀNH CÔNG (Đã lọc)
        long successfulBookings = bookingRepository.countSuccessfulBookings();
        long successfulSubscriptions = userSubscriptionRepository.countPaidActiveSubscriptions();
        
        long totalTransactions = successfulBookings + successfulSubscriptions;
        
        return DashboardResponseDTO.builder()
                .totalUsers(userRepository.count())
                .totalTransactions(totalTransactions)
                .totalFeedbacks(systemFeedbackRepository.count())
                .totalRevenue(totalRevenue)
                .build();
    }
    
    public Page<User> getPaginatedUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable);
    }

    // Lấy danh sách Giao dịch (Phân trang)
    public Map<String, Object> getTransactionHistory(int page, int size) {
        List<com.nutrimate.dto.TransactionDTO> allTransactions = new java.util.ArrayList<>();

        // 1. Lấy tất cả giao dịch Booking (chỉ lấy những đơn có orderCode)
        // Nếu file repository của bác chưa có hàm findAll thì dùng hàm mặc định
        List<com.nutrimate.entity.Booking> bookings = bookingRepository.findAll();
        for (com.nutrimate.entity.Booking b : bookings) {
            if (b.getOrderCode() != null) {
                allTransactions.add(com.nutrimate.dto.TransactionDTO.builder()
                        .orderCode(b.getOrderCode())
                        .userFullName(b.getMember() != null ? b.getMember().getFullName() : "Khách ẩn danh")
                        .userEmail(b.getMember() != null ? b.getMember().getEmail() : "")
                        .type("BOOKING")
                        .description("Đặt lịch chuyên gia " + (b.getExpert() != null ? b.getExpert().getUser().getFullName() : ""))
                        .amount(b.getFinalPrice() != null ? b.getFinalPrice().doubleValue() : 0.0)
                        .status(b.getStatus() != null ? b.getStatus().name() : "UNKNOWN")
                        // Nếu entity Booking của bác dùng tên biến khác (như createdAt), hãy đổi lại chỗ này:
                        .transactionDate(b.getBookingTime() != null ? b.getBookingTime() : java.time.LocalDateTime.now())
                        .build());
            }
        }

        // 2. Lấy tất cả giao dịch Mua gói Subscription (chỉ lấy những đơn có orderCode)
        List<com.nutrimate.entity.UserSubscription> subscriptions = userSubscriptionRepository.findAll();
        for (com.nutrimate.entity.UserSubscription s : subscriptions) {
            if (s.getOrderCode() != null) {
                allTransactions.add(com.nutrimate.dto.TransactionDTO.builder()
                        .orderCode(s.getOrderCode())
                        .userFullName(s.getUser() != null ? s.getUser().getFullName() : "Khách ẩn danh")
                        .userEmail(s.getUser() != null ? s.getUser().getEmail() : "")
                        .type("SUBSCRIPTION")
                        .description("Đăng ký gói " + (s.getPlan() != null ? s.getPlan().getPlanName() : ""))
                        .amount(s.getPlan() != null && s.getPlan().getPrice() != null ? s.getPlan().getPrice().doubleValue() : 0.0)
                        .status(s.getStatus() != null ? s.getStatus().name() : "UNKNOWN")
                        // Nếu chưa có startDate (PENDING), dùng thời gian hiện tại để sort không bị lỗi
                        .transactionDate(s.getStartDate() != null ? s.getStartDate() : java.time.LocalDateTime.now())
                        .build());
            }
        }

        // 3. Sắp xếp danh sách tổng hợp theo thời gian (Mới nhất lên đầu)
        allTransactions.sort((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));

        // 4. Xử lý phân trang (Pagination) thủ công bằng Java
        int totalItems = allTransactions.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int start = Math.min(page * size, totalItems);
        int end = Math.min(start + size, totalItems);

        List<com.nutrimate.dto.TransactionDTO> pagedTransactions = allTransactions.subList(start, end);

        // 5. Đóng gói trả về Frontend
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("transactions", pagedTransactions);
        response.put("currentPage", page);
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);

        return response;
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