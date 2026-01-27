package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "`Comments`") // Dùng backtick để match đúng bảng Comments trong MySQL, tránh bị hạ thành 'comments'
@Data
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Người comment

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false) // DB đang NOT NULL nên model cũng phải non-null để validate không lỗi
    private LocalDateTime createdAt = LocalDateTime.now();
}