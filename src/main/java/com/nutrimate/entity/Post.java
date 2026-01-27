package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Posts")
@Data
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "post_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Tác giả

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Cache số lượng để query cho nhanh (Denormalization)
    private Integer likeCount = 0;
    private Integer commentCount = 0;
}