package com.nutrimate.repository;

import com.nutrimate.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {
    // Lấy bài viết mới nhất trước
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}