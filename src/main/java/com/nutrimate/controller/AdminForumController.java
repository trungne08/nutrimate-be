package com.nutrimate.controller;

import com.nutrimate.dto.ForumDTO;
import com.nutrimate.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/forum")
@Tag(name = "11. Admin Forum Management", description = "Admin moderation for posts & comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminForumController {

    private final ForumService forumService;

    // 11.1 Admin xóa bài viết vi phạm tiêu chuẩn (hard delete)
    @Operation(summary = "[Admin] Delete post that violates community guidelines")
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<String> deletePostByAdmin(@PathVariable String id) {
        forumService.adminDeletePost(id);
        return ResponseEntity.ok("Post has been removed by admin.");
    }

    // 11.2 Admin ẩn bài viết (Không xóa hẳn - soft delete)
    @Operation(summary = "[Admin] Hide post (soft delete, keep record)")
    @PutMapping("/posts/{id}/hide")
    public ResponseEntity<ForumDTO.PostResponse> hidePostByAdmin(@PathVariable String id) {
        ForumDTO.PostResponse response = forumService.adminHidePost(id);
        return ResponseEntity.ok(response);
    }

    // 11.3 Admin xóa bình luận toxic / spam
    @Operation(summary = "[Admin] Delete toxic/spam comment")
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<String> deleteCommentByAdmin(@PathVariable String id) {
        forumService.adminDeleteComment(id);
        return ResponseEntity.ok("Comment has been removed by admin.");
    }
}

