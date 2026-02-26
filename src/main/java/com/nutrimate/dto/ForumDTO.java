package com.nutrimate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ForumDTO {

    // Request tạo/sửa bài viết
    @Data
    public static class PostRequest {
        private String content;
        private String imageUrl;
    }

    // Request tạo comment
    @Data
    public static class CommentRequest {
        private String content;
        private MultipartFile file;
    }

    // Response bài viết (Newsfeed)
    @Data
    @Builder
    public static class PostResponse {
        private String postId;
        private String authorName;
        private String authorAvatar;
        private String authorId;
        private String authorRole;
        private String content;
        private String imageUrl;
        private LocalDateTime createdAt;
        private Integer likeCount;
        private Integer commentCount;
        private boolean isLikedByCurrentUser; // Để hiển thị icon tim đỏ hay trắng
    }

    // Response chi tiết bài viết (Kèm comments)
    @Data
    @Builder
    public static class PostDetailResponse {
        private PostResponse post;
        private List<CommentResponse> comments;
    }

    // Response comment
    @Data
    @Builder
    public static class CommentResponse {
        private String commentId;
        private String authorName;
        private String authorAvatar;
        private String authorId;
        private String authorRole;
        private String content;
        private String imageUrl;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class UpdateCommentRequest {
        private String content;
        private MultipartFile file;
    }
}