package com.nutrimate.dto;

import com.nutrimate.entity.AiChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageDTO {
    private String id;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public static AiChatMessageDTO fromEntity(AiChatMessage m) {
        if (m == null) return null;
        return AiChatMessageDTO.builder()
                .id(m.getId())
                .role(m.getRole().name())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
