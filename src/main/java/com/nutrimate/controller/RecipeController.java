package com.nutrimate.controller;

import com.nutrimate.dto.RecipeDTO;
import com.nutrimate.entity.Recipe;
import com.nutrimate.entity.User;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
            return null;
        }

        String email = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt)
            email = ((Jwt) principal).getClaimAsString("email");
        else if (principal instanceof OidcUser)
            email = ((OidcUser) principal).getEmail();
        else if (principal instanceof OAuth2User)
            email = ((OAuth2User) principal).getAttribute("email");
        if (email == null)
            return null;
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
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
    @Operation(summary = "Get recipe detail (Public)")
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecipeDetail(@PathVariable String id, Authentication authentication) {

        boolean isVip = false;
        String userId = null;

        if (authentication != null && authentication.isAuthenticated()) {
            String cognitoId = null;
            
            // 1. LẤY MÃ COGNITO_ID (SUB) TỪ TOKEN
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                cognitoId = jwt.getSubject(); 
                if (cognitoId == null) {
                    cognitoId = jwt.getClaimAsString("username");
                }
            }
            if (cognitoId != null) {
                Optional<User> userOpt = userRepository.findByCognitoId(cognitoId);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userId = user.getId(); 
                    
                    if (user.getRole() != null) {
                        String roleName = user.getRole().name(); 
                        if (roleName.contains("ADMIN") || roleName.contains("EXPERT")) {
                            isVip = true;
                        }
                    }
                }
            }
        }
        if (isVip) {
            return ResponseEntity.ok(recipeService.getRecipeById(id));
        } else {
            return ResponseEntity.ok(recipeService.getRecipeById(id, userId));
        }
    }

    // --- ADMIN APIs ---
    @Operation(summary = "[Admin] Create new recipe with Image")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> createRecipe(
            @ModelAttribute RecipeDTO recipeDTO) {
        return ResponseEntity.ok(recipeService.createRecipe(recipeDTO, recipeDTO.getImageFile()));
    }

    @Operation(summary = "[Admin] Update recipe with Image")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Recipe> updateRecipe(
            @PathVariable String id,
            @ModelAttribute RecipeDTO recipeDTO) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, recipeDTO, recipeDTO.getImageFile()));
    }

    @Operation(summary = "[Admin] Delete recipe")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteRecipe(@PathVariable String id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok("Recipe deleted successfully");
    }
}