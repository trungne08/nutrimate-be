package com.nutrimate.controller;

import com.nutrimate.dto.RecipeDTO;
import com.nutrimate.entity.Recipe;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipe Management", description = "APIs for Recipe Library")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;

    // Helper
    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để xem công thức");
        }
        String email = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) email = ((Jwt) principal).getClaimAsString("email");
        else if (principal instanceof OidcUser) email = ((OidcUser) principal).getEmail();
        else if (principal instanceof OAuth2User) email = ((OAuth2User) principal).getAttribute("email");

        if (email == null) throw new BadRequestException("Token không hợp lệ");

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    // 4.1 Search Public
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

    // 4.2 Detail (Có check limit)
    @Operation(summary = "Get recipe detail (Limit 5/day for Free users)")
    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {

        // Gọi helper để lấy ID (nếu chưa login sẽ throw 400/404 tự động)
        String userId = getCurrentUserId(authentication);

        // Service sẽ tự throw ForbiddenException nếu quá giới hạn
        return ResponseEntity.ok(recipeService.getRecipeById(id, userId));
    }

    // --- ADMIN APIs ---
    @Operation(summary = "[Admin] Create new recipe")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> createRecipe(@Valid @RequestBody RecipeDTO recipeDTO) {
        return ResponseEntity.ok(recipeService.createRecipe(recipeDTO));
    }

    @Operation(summary = "[Admin] Update recipe")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable String id, @Valid @RequestBody RecipeDTO recipeDTO) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, recipeDTO));
    }

    @Operation(summary = "[Admin] Delete recipe")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteRecipe(@PathVariable String id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok("Recipe deleted successfully");
    }
}