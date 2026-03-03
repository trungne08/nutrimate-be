package com.nutrimate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO nhận từ Python AI Microservice.
 * Giả định response có dạng: {"response_text": "..."} hoặc {"message": "..."}
 */
@Data
public class AiCoachResponseDTO {
    @JsonProperty("response_text")
    private String responseText;
    private String message;

    public String getResponseText() {
        return responseText != null ? responseText : message;
    }
}
