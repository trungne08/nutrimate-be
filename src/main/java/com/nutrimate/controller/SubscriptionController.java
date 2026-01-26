package com.nutrimate.controller;

import com.nutrimate.entity.SubscriptionPlan;
import com.nutrimate.entity.UserSubscription;
import com.nutrimate.repository.SubscriptionPlanRepository;
import com.nutrimate.repository.UserSubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Subscription Management", description = "APIs for Plans and User Subscriptions")
public class SubscriptionController {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    public SubscriptionController(SubscriptionPlanRepository planRepository, UserSubscriptionRepository userSubscriptionRepository) {
        this.planRepository = planRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
    }

    // --- 3.1 PUBLIC: Lấy danh sách gói ---
    
    @Operation(summary = "Get all subscription plans")
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        // Chỉ lấy các gói đang Active
        // return ResponseEntity.ok(planRepository.findByIsActiveTrue());
        return ResponseEntity.ok(planRepository.findAll());
    }

    // --- 3.2 - 3.4 ADMIN: CRUD Gói cước ---

    @Operation(summary = "[Admin] Create new plan")
    @PostMapping("/admin/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPlan(@RequestBody SubscriptionPlan plan) {
        SubscriptionPlan savedPlan = planRepository.save(plan);
        return ResponseEntity.ok(savedPlan);
    }

    @Operation(summary = "[Admin] Update plan")
    @PutMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePlan(@PathVariable String id, @RequestBody SubscriptionPlan planRequest) {
        return planRepository.findById(id).map(plan -> {
            plan.setPlanName(planRequest.getPlanName());
            plan.setPrice(planRequest.getPrice());
            plan.setDurationDays(planRequest.getDurationDays());
            plan.setCanUseAiCoach(planRequest.getCanUseAiCoach());
            plan.setIsExpertPlan(planRequest.getIsExpertPlan());
            // ... update other fields
            return ResponseEntity.ok(planRepository.save(plan));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "[Admin] Soft delete/Hide plan")
    @DeleteMapping("/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePlan(@PathVariable String id) {
        return planRepository.findById(id).map(plan -> {
            plan.setIsActive(false); // Ẩn gói đi
            planRepository.save(plan);
            return ResponseEntity.ok(Map.of("message", "Plan deactivated successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- 3.5 & 3.6 MEMBER: Mua & Kiểm tra gói ---

    @Operation(summary = "Get my current subscription")
    @GetMapping("/subscription")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMySubscription() {
        // Logic: Lấy user hiện tại từ SecurityContext
        // Tìm subscription active trong DB
        return ResponseEntity.ok(Map.of("message", "Current subscription details here..."));
    }

    @Operation(summary = "Subscribe to a plan (Payment)")
    @PostMapping("/subscription")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Object> request) {
        String planId = String.valueOf(request.get("planId").toString());
        String paymentMethod = (String) request.get("paymentMethod");

        // Logic:
        // 1. Validate Plan
        // 2. Tạo Payment Link (Momo/ZaloPay giả lập)
        // 3. Trả về URL thanh toán cho Frontend
        
        String fakePaymentUrl = "https://momo.vn/pay?orderId=123";
        return ResponseEntity.ok(Map.of(
            "paymentUrl", fakePaymentUrl,
            "message", "Redirect user to this URL to pay"
        ));
    }
}