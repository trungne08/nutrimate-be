package com.nutrimate.controller;

import com.nutrimate.dto.DashboardResponseDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Feedback;
import com.nutrimate.entity.User;
import com.nutrimate.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @Operation(summary = "[Admin] Lấy số liệu tổng quan")
    @GetMapping("/stats")
    public ResponseEntity<DashboardResponseDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    // 2. API Danh sách User (Phân trang - Mặc định size 20)
    @Operation(summary = "[Admin] Lấy danh sách User có phân trang")
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(dashboardService.getPaginatedUsers(page, size));
    }

    // 3. API Danh sách Giao dịch (Phân trang - Mặc định size 20)
    @Operation(summary = "[Admin] Lịch sử giao dịch (Gộp chung Booking và Subscription)")
    @GetMapping("/transactions")
    // @PreAuthorize("hasRole('ADMIN')") // Mở comment này ra nếu bác đang dùng Spring Security chặn quyền
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return ResponseEntity.ok(dashboardService.getTransactionHistory(page, size));
    }

    @Operation(summary = "[Admin] Lấy danh sách Feedback có phân trang")
    @GetMapping("/feedbacks")
    public ResponseEntity<Page<Feedback>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) { // Mặc định size 10 như bạn muốn
        
        return ResponseEntity.ok(dashboardService.getPaginatedFeedbacks(page, size));
    }

    @Operation(summary = "[Admin Chart 1] Biểu đồ tăng trưởng User (Tham số period: month, year)")
    @GetMapping("/charts/users")
    public ResponseEntity<List<com.nutrimate.dto.ChartDTO>> getUserGrowthChart(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(dashboardService.getUserGrowthChart(period));
    }

    @Operation(summary = "[Admin Chart 2] Biểu đồ tròn cơ cấu gói Subscription")
    @GetMapping("/charts/subscriptions")
    public ResponseEntity<List<com.nutrimate.dto.ChartDTO>> getSubscriptionPieChart() {
        return ResponseEntity.ok(dashboardService.getSubscriptionPieChart());
    }

    @Operation(summary = "[Admin Chart 3] Biểu đồ Doanh thu gộp (Tham số period: week, month, year)")
    @GetMapping("/charts/revenue")
    public ResponseEntity<List<com.nutrimate.dto.ChartDTO>> getRevenueGrowthChart(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(dashboardService.getRevenueGrowthChart(period));
    }
}