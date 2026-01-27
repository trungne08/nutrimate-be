package com.nutrimate.repository;

import com.nutrimate.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, String> {
    // Tìm like của user cho bài viết cụ thể
    Optional<PostLike> findByUserIdAndPostId(String userId, String postId);
    
    // Check xem user đã like chưa
    boolean existsByUserIdAndPostId(String userId, String postId);
}