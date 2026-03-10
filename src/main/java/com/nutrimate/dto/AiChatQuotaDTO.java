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
public class AiChatQuotaDTO {
    @JsonProperty("daily_limit")
    private int dailyLimit;

    @JsonProperty("used_today")
    private int usedToday;

    @JsonProperty("remaining_chats")
    private int remainingChats;
}
