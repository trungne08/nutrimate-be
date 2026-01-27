package com.nutrimate.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString; // Import thêm để tránh lỗi vòng lặp
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

    // Cache số lượng để query cho nhanh
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;
    
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    // 👇 THÊM ĐOẠN NÀY: Cấu hình Cascade để tự động xóa Comment & Like khi xóa Post
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // Quan trọng: Ngăn chặn lỗi StackOverflow do vòng lặp toString()
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // Quan trọng
    private List<PostLike> likes = new ArrayList<>();
}