package com.nutrimate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSendResponseDTO {
    private String response;

    @JsonProperty("remaining_chats")
    private int remainingChats;

    @JsonProperty("daily_limit")
    private int dailyLimit;
}
