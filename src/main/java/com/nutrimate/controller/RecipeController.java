package com.nutrimate.controller;

import com.nutrimate.dto.RecipeDTO;
import com.nutrimate.entity.Recipe;
import com.nutrimate.service.RecipeService;
import com.nutrimate.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nutrimate.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipe Management", description = "APIs for Recipe Library (Admin manages, User views)")
public class RecipeController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;

    public RecipeController(RecipeService recipeService, UserRepository userRepository) {
        this.recipeService = recipeService;
        this.userRepository = userRepository;
    }

    // --- PUBLIC APIs (Ai cũng xem được) ---

    // 4.1 GET /api/recipes
    @Operation(summary = "Search recipes (Public)")
    @GetMapping
    public ResponseEntity<Page<Recipe>> getRecipes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer maxCal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(recipeService.getRecipes(keyword, maxCal, pageable));
    }

    // 4.2 GET /api/recipes/{id}
    @Operation(summary = "Get recipe detail (Limit 5/day for Free users)")
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecipeById(
            @PathVariable String id,
            // 👇 Lấy thông tin user từ Token (Cognito)
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {

        // 1. Lấy email từ Token
        String email = null;
        if (oidcUser != null) {
            email = oidcUser.getEmail();
        } else if (oauth2User != null) {
            email = oauth2User.getAttribute("email");
        }

        // 2. Nếu chưa đăng nhập -> Báo lỗi 401
        if (email == null) {
            return ResponseEntity.status(401).body("Unauthorized: Please login to view recipes.");
        }

        // 3. Tìm User trong DB bằng email để lấy userId
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found in database. Please login again to sync.");
        }

        String userId = userOpt.get().getId();

        // 4. Gọi Service kèm userId để check giới hạn xem
        try {
            Recipe recipe = recipeService.getRecipeById(id, userId);
            return ResponseEntity.ok(recipe);
        } catch (RuntimeException e) {
            // Nếu quá giới hạn 5 bài -> Trả về lỗi 403 Forbidden
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
    // --- ADMIN APIs (Chỉ Admin được thao tác) ---

    // 4.3 POST /api/recipes
    @Operation(summary = "[Admin] Create new recipe")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // 👈 CHỐT: Chỉ Admin được tạo
    public ResponseEntity<Recipe> createRecipe(@Valid @RequestBody RecipeDTO recipeDTO) {
        return ResponseEntity.ok(recipeService.createRecipe(recipeDTO));
    }

    // 4.4 PUT /api/recipes/{id}
    @Operation(summary = "[Admin] Update recipe")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 👈 CHỐT: Chỉ Admin được sửa
    public ResponseEntity<Recipe> updateRecipe(@PathVariable String id, @Valid @RequestBody RecipeDTO recipeDTO) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, recipeDTO));
    }

    // 4.5 DELETE /api/recipes/{id}
    @Operation(summary = "[Admin] Delete recipe")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 👈 CHỐT: Chỉ Admin được xóa
    public ResponseEntity<String> deleteRecipe(@PathVariable String id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok("Recipe deleted successfully");
    }
}