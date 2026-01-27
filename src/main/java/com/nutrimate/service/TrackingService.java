package com.nutrimate.service;

import com.nutrimate.dto.DailyLogResponseDTO;
import com.nutrimate.dto.TrackingRequestDTO;
import com.nutrimate.entity.*;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final DailyLogRepository dailyLogRepository;
    private final MealLogRepository mealLogRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    // 7.1 Lấy log theo ngày
    public DailyLogResponseDTO getLogByDate(String userId, LocalDate date) {
        Optional<DailyLog> logOpt = dailyLogRepository.findByUserIdAndLogDate(userId, date);

        if (logOpt.isEmpty()) {
            // Chưa có log -> Trả về object rỗng
            DailyLogResponseDTO empty = new DailyLogResponseDTO();
            empty.setDate(date);
            empty.setTotalCalories(0);
            empty.setMeals(new ArrayList<>());
            return empty;
        }

        return mapToDTO(logOpt.get());
    }

    // 7.2 Thêm món ăn vào nhật ký
    @Transactional
    public DailyLogResponseDTO addFoodLog(String userId, TrackingRequestDTO.AddFoodLog req) {
        DailyLog dailyLog = dailyLogRepository.findByUserIdAndLogDate(userId, req.getDate())
                .orElseGet(() -> {
                    DailyLog newLog = new DailyLog();
                    newLog.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
                    newLog.setLogDate(req.getDate());
                    newLog.setTotalCaloriesIn(0);
                    return dailyLogRepository.save(newLog);
                });

        Recipe recipe = recipeRepository.findById(req.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found"));

        MealLog mealLog = new MealLog();
        mealLog.setDailyLog(dailyLog);
        mealLog.setRecipe(recipe);
        mealLog.setMealType(req.getMealType());
        mealLog.setAmount(req.getAmount());
        
        int calConsumed = (int) (recipe.getCalories() * req.getAmount());
        mealLog.setCaloriesConsumed(calConsumed);

        mealLogRepository.save(mealLog);
        updateDailyTotal(dailyLog);

        return mapToDTO(dailyLog);
    }

    // 7.3 Update Food Log
    @Transactional
    public DailyLogResponseDTO updateFoodLog(String userId, String mealLogId, TrackingRequestDTO.UpdateFoodLog req) {
        MealLog mealLog = mealLogRepository.findById(mealLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Food log entry not found"));

        if (!mealLog.getDailyLog().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to edit this log");
        }

        mealLog.setAmount(req.getAmount());
        int newCal = (int) (mealLog.getRecipe().getCalories() * req.getAmount());
        mealLog.setCaloriesConsumed(newCal);
        
        mealLogRepository.save(mealLog);
        updateDailyTotal(mealLog.getDailyLog());

        return mapToDTO(mealLog.getDailyLog());
    }

    // 7.4 Delete Food Log
    @Transactional
    public void deleteFoodLog(String userId, String mealLogId) {
        MealLog mealLog = mealLogRepository.findById(mealLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Food log entry not found"));

        if (!mealLog.getDailyLog().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to delete this log");
        }
        
        DailyLog parentLog = mealLog.getDailyLog();
        mealLogRepository.delete(mealLog);
        
        parentLog.setTotalCaloriesIn(parentLog.getTotalCaloriesIn() - mealLog.getCaloriesConsumed());
        dailyLogRepository.save(parentLog);
    }

    // Helper: Tính lại tổng calo của DailyLog từ DB
    private void updateDailyTotal(DailyLog dailyLog) {
        // Lấy danh sách meal hiện tại
        // Lưu ý: Nếu vừa save MealLog xong, list meals trong dailyLog có thể chưa update
        // Nên query lại từ DB cho chắc
        Integer total = mealLogRepository.findAll().stream()
                .filter(m -> m.getDailyLog().getId().equals(dailyLog.getId()))
                .mapToInt(MealLog::getCaloriesConsumed)
                .sum();
        
        dailyLog.setTotalCaloriesIn(total);
        dailyLogRepository.save(dailyLog);
    }

    // Helper: Map Entity -> DTO
    private DailyLogResponseDTO mapToDTO(DailyLog log) {
        DailyLogResponseDTO dto = new DailyLogResponseDTO();
        dto.setLogId(log.getId());
        dto.setDate(log.getLogDate());
        dto.setTotalCalories(log.getTotalCaloriesIn());
        
        // Query meal logs (Nếu chưa fetch EAGER thì cần repo call hoặc transactional)
        // Giả sử logic fetch đã ổn hoặc dùng lazy load trong transaction
        // Cách an toàn là query list meal log
        dto.setMeals(mealLogRepository.findAll().stream()
                .filter(m -> m.getDailyLog().getId().equals(log.getId()))
                .map(m -> {
                    DailyLogResponseDTO.MealLogDTO md = new DailyLogResponseDTO.MealLogDTO();
                    md.setMealLogId(m.getId());
                    md.setRecipeId(m.getRecipe().getId());
                    md.setRecipeName(m.getRecipe().getTitle());
                    md.setMealType(m.getMealType());
                    md.setAmount(m.getAmount());
                    md.setCalories(m.getCaloriesConsumed());
                    return md;
                }).collect(Collectors.toList()));
        
        return dto;
    }
}