package com.nutrimate.controller;

import com.nutrimate.dto.ApiResponse; // Giả sử bạn có class bọc response chung
import com.nutrimate.dto.ExpertApproveRequest;
import com.nutrimate.entity.User;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.entity.ExpertProfile;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.repository.ExpertProfileRepository;
import com.nutrimate.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
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

    @Operation(summary = "Get list of Pending Experts")
    @GetMapping("/experts/pending")
    public ResponseEntity<List<ExpertProfile>> getPendingExperts() {
        // 👇 CODE CŨ (SAI): 
        // return ResponseEntity.ok(Map.of("message", "List of pending experts"));

        // 👇 CODE MỚI (ĐÚNG): Gọi DB lấy danh sách PENDING thật
        List<ExpertProfile> pendingList = expertProfileRepository.findByStatus(ExpertProfile.ApprovalStatus.PENDING);
        
        if (pendingList.isEmpty()) {
            return ResponseEntity.noContent().build(); // Trả về 204 nếu không có ai chờ duyệt
        }
        
        return ResponseEntity.ok(pendingList);
    }

    @Operation(summary = "Approve or Reject Expert (Status: APPROVED / REJECTED)")
    @PutMapping("/experts/{id}/approve")
    @Transactional
    public ResponseEntity<?> approveExpert(
            @PathVariable String id, 
            @RequestBody ExpertApproveRequest request) { // 👈 Đã sửa chỗ này
        
        String statusStr = request.getStatus(); // Lấy từ DTO
        
        ExpertProfile expert = expertProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expert profile not found"));

        if ("APPROVED".equalsIgnoreCase(statusStr)) {
            expert.setStatus(ExpertProfile.ApprovalStatus.APPROVED);
            
            // Nâng cấp User lên Role EXPERT
            User user = expert.getUser();
            user.setRole(User.UserRole.EXPERT);
            userRepository.save(user);
            
        } else if ("REJECTED".equalsIgnoreCase(statusStr)) {
            expert.setStatus(ExpertProfile.ApprovalStatus.REJECTED);
        } else {
            throw new BadRequestException("Status không hợp lệ. Vui lòng điền 'APPROVED' hoặc 'REJECTED'");
        }
        expertProfileRepository.save(expert);
        return ResponseEntity.ok(Map.of(
            "message", "Đã cập nhật trạng thái thành công!",
            "status", expert.getStatus(),
            "expertId", expert.getId()
        ));
    }
}