package com.nutrimate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInHistoryResponse {
    
    private LocalDateTime checkInTime; 
    
    private boolean checkin;         
}