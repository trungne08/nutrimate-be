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
    @Operation(summary = "[Admin] Lấy danh sách Giao dịch có phân trang")
    @GetMapping("/transactions")
    public ResponseEntity<Page<Booking>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(dashboardService.getPaginatedTransactions(page, size));
    }

    @Operation(summary = "[Admin] Lấy danh sách Feedback có phân trang")
    @GetMapping("/feedbacks")
    public ResponseEntity<Page<Feedback>> getFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) { // Mặc định size 10 như bạn muốn
        
        return ResponseEntity.ok(dashboardService.getPaginatedFeedbacks(page, size));
    }
}