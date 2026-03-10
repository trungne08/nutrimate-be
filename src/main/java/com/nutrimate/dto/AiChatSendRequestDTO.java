package com.nutrimate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatSendRequestDTO {
    @NotBlank(message = "Message is required")
    private String message;
}
