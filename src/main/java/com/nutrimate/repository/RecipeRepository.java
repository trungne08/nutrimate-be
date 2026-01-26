package com.nutrimate.repository;

import com.nutrimate.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, String> {
    // Tìm kiếm công thức theo tên (có phân trang)
    Page<Recipe> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE " +
            "(:keyword IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:maxCal IS NULL OR r.calories <= :maxCal)")
    Page<Recipe> searchRecipes(@Param("keyword") String keyword,
            @Param("maxCal") Integer maxCal,
            Pageable pageable);

    // Lọc công thức Premium
    Page<Recipe> findByIsPremium(Boolean isPremium, Pageable pageable);
}