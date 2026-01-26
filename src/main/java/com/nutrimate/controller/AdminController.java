package com.nutrimate.controller;

import com.nutrimate.dto.ApiResponse; // Giả sử bạn có class bọc response chung
import com.nutrimate.entity.User;
import com.nutrimate.entity.ExpertProfile;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.repository.ExpertProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Management", description = "APIs for managing Users and Experts")
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới gọi được
public class AdminController {

    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;

    public AdminController(UserRepository userRepository, ExpertProfileRepository expertProfileRepository) {
        this.userRepository = userRepository;
        this.expertProfileRepository = expertProfileRepository;
    }

    // --- 2.1 & 2.2 QUẢN LÝ USER ---

    @Operation(summary = "Get list of users", description = "Get users with pagination and role filter")
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) User.UserRole role) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;
        
        if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user details")
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserDetail(@PathVariable String id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Ban/Unban User")
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable String id, @RequestBody Map<String, String> statusRequest) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        // Lưu ý: Cần thêm cột 'is_active' hoặc 'status' vào entity User nếu chưa có
        // user.setIsActive("ACTIVE".equals(statusRequest.get("status")));
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "User status updated to " + statusRequest.get("status")));
    }

    @Operation(summary = "Soft Delete User")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = userOpt.get();
        // Soft delete: Đánh dấu đã xóa thay vì xóa thật
        // user.setDeleted(true); 
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "User soft deleted successfully"));
    }

    // --- 2.5 & 2.6 QUẢN LÝ EXPERT (DUYỆT) ---

    @Operation(summary = "Get pending experts")
    @GetMapping("/experts/pending")
    public ResponseEntity<?> getPendingExperts() {
        // Cần custom query trong Repo để tìm expert chưa được duyệt
        // Ví dụ: return expertProfileRepository.findByStatus("PENDING");
        return ResponseEntity.ok(Map.of("message", "List of pending experts"));
    }

    @Operation(summary = "Approve or Reject Expert")
    @PutMapping("/experts/{id}/approve")
    public ResponseEntity<?> approveExpert(@PathVariable String id, @RequestBody Map<String, String> request) {
        String status = request.get("status"); // APPROVED or REJECTED
        
        // Logic: Tìm expert profile và update status
        // ExpertProfile expert = expertProfileRepository.findById(id)...
        // expert.setStatus(status);
        // expertProfileRepository.save(expert);
        
        // Nếu Approved, có thể cần update role của User thành EXPERT
        
        return ResponseEntity.ok(Map.of("message", "Expert application " + status));
    }
}