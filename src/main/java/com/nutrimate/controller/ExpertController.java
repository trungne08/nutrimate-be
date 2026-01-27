package com.nutrimate.controller;

import com.nutrimate.entity.ExpertProfile;
import com.nutrimate.service.ExpertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/experts")
@Tag(name = "5. Expert Profile", description = "Public APIs to find experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;

    // 5.1 GET /api/experts (Filter)
    @Operation(summary = "Search experts")
    @GetMapping
    public ResponseEntity<List<ExpertProfile>> searchExperts(
            @RequestParam(required = false) Float minRating,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(expertService.searchExperts(minRating, maxPrice));
    }

    // 5.2 GET /api/experts/{id}
    @Operation(summary = "Get expert profile")
    @GetMapping("/{id}")
    public ResponseEntity<ExpertProfile> getExpertDetail(@PathVariable String id) {
        return ResponseEntity.ok(expertService.getExpertById(id));
    }
}