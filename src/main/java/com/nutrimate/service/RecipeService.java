package com.nutrimate.service;

import com.nutrimate.dto.RecipeDTO;
import com.nutrimate.entity.*;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserBenefitUsageRepository benefitUsageRepository;
    private final UserRepository userRepository;

    // 1. Tìm kiếm (Giữ nguyên)
    public Page<Recipe> getRecipes(String keyword, Integer maxCal, Pageable pageable) {
        return recipeRepository.searchRecipes(keyword, maxCal, pageable);
    }

    // 👇 HÀM 1: Lấy Recipe thuần túy (Dùng cho Admin Update/Delete hoặc Internal)
    public Recipe getRecipeById(String id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
    }

    // 👇 HÀM 2: Lấy Recipe + Check giới hạn (Dùng cho User xem chi tiết)
    @Transactional
    public Recipe getRecipeById(String recipeId, String userId) {
        checkAndIncrementFreeLimit(userId);
        return getRecipeById(recipeId);
    }

    private void checkAndIncrementFreeLimit(String userId) {
        Optional<UserSubscription> activeSubOpt = userSubscriptionRepository.findActiveSubscriptionByUserId(userId);
        boolean isPremium = false;
        
        if (activeSubOpt.isPresent()) {
            String planName = activeSubOpt.get().getPlan().getPlanName().toUpperCase();
            if (planName.contains("PREMIUM") || planName.contains("EXPERT")) {
                isPremium = true; 
            }
        }

        if (isPremium) return;

        UserBenefitUsage usage = benefitUsageRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserBenefitUsage newUsage = new UserBenefitUsage();
                    newUsage.setUserId(userId);
                    newUsage.setDailyRecipeViews(0);
                    newUsage.setLastRecipeViewDate(LocalDate.now());
                    return newUsage;
                });

        if (usage.getLastRecipeViewDate() == null || !usage.getLastRecipeViewDate().equals(LocalDate.now())) {
            usage.setDailyRecipeViews(0);
            usage.setLastRecipeViewDate(LocalDate.now());
        }

        if (usage.getDailyRecipeViews() >= 5) {
            throw new ForbiddenException("DAILY_LIMIT_REACHED: You have reached your free daily limit (5/5). Please upgrade to Premium.");
        }

        usage.setDailyRecipeViews(usage.getDailyRecipeViews() + 1);
        benefitUsageRepository.save(usage);
    }

    // --- CÁC HÀM ADMIN (Giờ sẽ gọi hàm 1 tham số -> HẾT LỖI) ---

    @Transactional
    public Recipe createRecipe(RecipeDTO dto) {
        Recipe recipe = new Recipe();
        mapDtoToEntity(dto, recipe);
        return recipeRepository.save(recipe);
    }

    @Transactional
    public Recipe updateRecipe(String id, RecipeDTO dto) {
        Recipe recipe = getRecipeById(id); // 👈 Giờ nó gọi hàm 1 (OK)
        mapDtoToEntity(dto, recipe);
        return recipeRepository.save(recipe);
    }

    @Transactional
    public void deleteRecipe(String id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recipe not found to delete");
        }
        recipeRepository.deleteById(id);
    }

    // Helper Map
    private void mapDtoToEntity(RecipeDTO dto, Recipe recipe) {
        recipe.setTitle(dto.getTitle());
        recipe.setDescription(dto.getDescription());
        recipe.setInstruction(dto.getInstruction());
        recipe.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        recipe.setCalories(dto.getCalories());
        recipe.setProtein(dto.getProtein());
        recipe.setCarbs(dto.getCarbs());
        recipe.setFat(dto.getFat());
        recipe.setIsPremium(dto.getIsPremium());
    }
}