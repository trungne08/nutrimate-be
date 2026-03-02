package com.nutrimate.dto;

import com.nutrimate.entity.ChatType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentChatResponse {

    private String partnerId;
    private String partnerName;
    private String partnerAvatar;
    private String lastMessage;
    private LocalDateTime timestamp;
    private ChatType chatType;
}

