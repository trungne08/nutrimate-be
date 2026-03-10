package com.nutrimate.service;

import com.nutrimate.dto.AiCoachRequestDTO;
import com.nutrimate.dto.AiCoachResponseDTO;
import com.nutrimate.entity.HealthProfile;
import com.nutrimate.entity.Recipe;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.HealthProfileRepository;
import com.nutrimate.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCoachService {

    @Value("${app.ai.service.url:http://localhost:8000/api/ai/chat}")
    private String aiServiceUrl;

    private final HealthProfileRepository healthProfileRepository;
    private final RecipeRepository recipeRepository;
    private final RestTemplate restTemplate;

    public String getAdviceFromAi(String userId, String userMessage) {
        return getAdviceFromAiWithContext(userId, userMessage, null);
    }

    public String getAdviceFromAiWithContext(String userId, String userMessage, java.util.List<AiCoachRequestDTO.ChatMessageDTO> chatHistory) {
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Health profile not found. Please complete your profile first."));

        List<Recipe> recipes = recipeRepository.findRandomRecipes();
        List<AiCoachRequestDTO.AvailableRecipeDTO> availableRecipes = recipes.stream()
                .map(r -> AiCoachRequestDTO.AvailableRecipeDTO.builder()
                        .recipeName(r.getTitle())
                        .calories(r.getCalories() != null ? r.getCalories() : 0)
                        .description(r.getDescription() != null ? r.getDescription() : "")
                        .build())
                .collect(Collectors.toList());

        AiCoachRequestDTO.HealthProfileDTO healthProfileDto = AiCoachRequestDTO.HealthProfileDTO.builder()
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .heightCm(profile.getHeightCm())
                .weightKg(profile.getWeightKg())
                .targetWeightKg(profile.getTargetWeightKg())
                .activityLevel(profile.getActivityLevel())
                .dietaryPreference(profile.getDietaryPreference())
                .build();

        AiCoachRequestDTO request = AiCoachRequestDTO.builder()
                .userMessage(userMessage)
                .healthProfile(healthProfileDto)
                .availableRecipes(availableRecipes)
                .chatHistory(chatHistory != null ? chatHistory : java.util.Collections.emptyList())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiCoachRequestDTO> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AiCoachResponseDTO> response = restTemplate.exchange(
                    aiServiceUrl,
                    HttpMethod.POST,
                    entity,
                    AiCoachResponseDTO.class
            );
            AiCoachResponseDTO body = response.getBody();
            if (body != null && body.getResponseText() != null) {
                return body.getResponseText();
            }
            return "Không nhận được phản hồi từ AI.";
        } catch (Exception e) {
            log.error("Error calling AI service: {}", e.getMessage());
            throw new RuntimeException("Không thể kết nối tới AI Coach. Vui lòng thử lại sau.");
        }
    }
}
