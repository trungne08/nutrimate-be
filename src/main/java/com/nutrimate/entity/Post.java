package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`Posts`")
@Data
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "post_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Tác giả

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Cache số lượng để query cho nhanh (Denormalization)
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;
}