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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCoachService {

    private static final int RAG_RECIPE_LIMIT = 10;
    private static final Set<String> STOP_WORDS = Set.of(
            "tôi", "mình", "muốn", "có", "gì", "món", "ăn", "không", "làm", "thế", "nào", "cho", "với", "về",
            "i", "want", "have", "get", "make", "recipe", "for", "with", "about", "the", "a", "an", "and", "or"
    );

    @Value("${app.ai.service.url:http://localhost:8000/api/ai/chat}")
    private String aiServiceUrl;

    @Value("${app.frontend.url:https://www.nutrimate.site}")
    private String frontendUrl;

    private final HealthProfileRepository healthProfileRepository;
    private final RecipeRepository recipeRepository;
    private final RestTemplate restTemplate;

    public String getAdviceFromAi(String userId, String userMessage) {
        return getAdviceFromAiWithContext(userId, userMessage, null);
    }

    public String getAdviceFromAiWithContext(String userId, String userMessage, java.util.List<AiCoachRequestDTO.ChatMessageDTO> chatHistory) {
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Health profile not found. Please complete your profile first."));

        List<Recipe> recipes = fetchRelevantRecipes(userMessage);
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

        String systemContext = buildRecipeContextString(recipes);

        AiCoachRequestDTO request = AiCoachRequestDTO.builder()
                .userMessage(userMessage)
                .healthProfile(healthProfileDto)
                .availableRecipes(availableRecipes)
                .chatHistory(chatHistory != null ? chatHistory : java.util.Collections.emptyList())
                .systemContext(systemContext)
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

    private List<Recipe> fetchRelevantRecipes(String userMessage) {
        String keyword = extractSearchKeyword(userMessage);
        if (keyword != null && !keyword.isBlank()) {
            List<Recipe> byKeyword = recipeRepository.findByKeywordInTitleOrDescription(
                    keyword, PageRequest.of(0, RAG_RECIPE_LIMIT));
            if (!byKeyword.isEmpty()) return byKeyword;
        }
        List<Recipe> random = recipeRepository.findRandomRecipes();
        return random.size() > RAG_RECIPE_LIMIT ? random.subList(0, RAG_RECIPE_LIMIT) : random;
    }

    private String extractSearchKeyword(String message) {
        if (message == null || message.isBlank()) return null;
        String normalized = message.toLowerCase().trim();
        String[] words = Pattern.compile("\\s+").split(normalized);
        List<String> meaningful = new ArrayList<>();
        for (String w : words) {
            if (w.length() >= 3 && !STOP_WORDS.contains(w)) {
                meaningful.add(w);
                if (meaningful.size() >= 2) break;
            }
        }
        return meaningful.isEmpty() ? null : String.join(" ", meaningful);
    }

    private String buildRecipeContextString(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) return "";
        String base = frontendUrl.replaceAll("/$", "");
        StringBuilder sb = new StringBuilder();
        sb.append("[DANH SÁCH CÔNG THỨC NỘI BỘ NUTRIMATE]:\n");
        for (Recipe r : recipes) {
            String cal = r.getCalories() != null ? r.getCalories() + " kcal" : "N/A";
            String desc = r.getDescription() != null ? truncate(r.getDescription(), 150) : "";
            String link = base + "/recipes/" + (r.getId() != null ? r.getId() : "");
            sb.append("- Món: ").append(r.getTitle() != null ? r.getTitle() : "N/A")
                    .append(" | Calo: ").append(cal)
                    .append(" | Mô tả: ").append(desc)
                    .append(" | Link: ").append(link).append("\n");
        }
        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
