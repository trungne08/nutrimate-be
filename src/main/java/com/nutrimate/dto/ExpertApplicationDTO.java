package com.nutrimate.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExpertApplicationDTO {
    private String specialization;
    private String bio;
    private Integer yearsExperience;
    private BigDecimal hourlyRate;
    // Ảnh bằng cấp (file) sẽ được hứng riêng trong Controller bằng MultipartFile
}