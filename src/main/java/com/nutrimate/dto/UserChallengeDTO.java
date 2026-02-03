package com.nutrimate.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserChallengeDTO {
    private String id;              // ID bảng UserChallenge
    private String challengeId;     // ID bảng Challenge gốc
    private String challengeTitle;  // Tên thử thách
    private Integer daysCompleted;  // Đã làm được bao nhiêu ngày
    private Integer totalDays;      // Tổng số ngày của thử thách
    private Integer progressPercent; // % hoàn thành (0 - 100)
    private LocalDate joinDate;
    private String status;
}