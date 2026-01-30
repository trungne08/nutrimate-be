package com.nutrimate.controller;

import com.nutrimate.dto.ExpertApplicationDTO;
import com.nutrimate.entity.ExpertProfile;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.ExpertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/experts")
@Tag(name = "Expert Profile", description = "Public APIs to find experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;
    private final UserRepository userRepository;

    // 5.1 GET /api/experts (Filter)
    @Operation(summary = "Search experts")
    @GetMapping
    public ResponseEntity<List<ExpertProfile>> searchExperts(
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(expertService.searchExperts(minRating, maxPrice));
    }

    // 5.2 GET /api/experts/{id}
    @Operation(summary = "Get expert profile")
    @GetMapping("/{id}")
    public ResponseEntity<ExpertProfile> getExpertDetail(@PathVariable String id) {
        return ResponseEntity.ok(expertService.getExpertById(id));
    }

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện chức năng này");
        }

        String email = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt) {
            email = ((Jwt) principal).getClaimAsString("email");
        } else if (principal instanceof OidcUser) {
            email = ((OidcUser) principal).getEmail();
        } else if (principal instanceof OAuth2User) {
            email = ((OAuth2User) principal).getAttribute("email");
        }

        if (email == null) {
            throw new BadRequestException("Token không hợp lệ (Không tìm thấy email)");
        }

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }
    
    @Operation(summary = "Apply to become an Expert")
    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ExpertProfile> applyToBecomeExpert(
            @RequestParam("specialization") String specialization,
            @RequestParam("bio") String bio,
            @RequestParam("yearsExperience") Integer yearsExperience,
            @RequestParam("hourlyRate") BigDecimal hourlyRate,
            @RequestParam(value = "certificateFile", required = false) MultipartFile certificateFile,
            @Parameter(hidden = true) Authentication authentication) {

        ExpertApplicationDTO dto = new ExpertApplicationDTO();
        dto.setSpecialization(specialization);
        dto.setBio(bio);
        dto.setYearsExperience(yearsExperience);
        dto.setHourlyRate(hourlyRate);
        // File sẽ được xử lý riêng trong Service

        String userId = getCurrentUserId(authentication);
        
        return ResponseEntity.ok(expertService.submitApplication(userId, dto, certificateFile));
    }
}