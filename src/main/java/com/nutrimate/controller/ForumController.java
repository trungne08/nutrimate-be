package com.nutrimate.controller;

import com.nutrimate.dto.ForumDTO;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/forum")
@Tag(name = "Community Forum", description = "Social features: Posts, Likes, Comments")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final UserRepository userRepository;

    // Helper: Lấy User ID chuẩn
    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Vui lòng đăng nhập để thực hiện chức năng này");
        }

        String email = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt) {
            email = ((Jwt) principal).getClaimAsString("email");
        } else if (principal instanceof OidcUser) {
            email = ((OidcUser) principal).getEmail();
        } else if (principal instanceof OAuth2User) {
            email = ((OAuth2User) principal).getAttribute("email");
        }

        if (email == null) {
            throw new BadRequestException("Không tìm thấy email trong thông tin xác thực");
        }

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại trong hệ thống"));
    }

    // 10.1 GET Newsfeed (Cho phép Guest)
    @Operation(summary = "Get Newsfeed (List posts)")
    @GetMapping("/posts")
    public ResponseEntity<Page<ForumDTO.PostResponse>> getNewsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) Authentication authentication) {
        
        String userId = null;
        // Nếu đã login thì lấy ID để check trạng thái like
        if (authentication != null && authentication.isAuthenticated()) {
            try { userId = getCurrentUserId(authentication); } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(forumService.getNewsFeed(userId, PageRequest.of(page, size)));
    }

    // 10.2 GET Detail
    @Operation(summary = "Get Post Detail + Comments")
    @GetMapping("/posts/{id}")
    public ResponseEntity<ForumDTO.PostDetailResponse> getPostDetail(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        String userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try { userId = getCurrentUserId(authentication); } catch (Exception ignored) {}
        }
        
        return ResponseEntity.ok(forumService.getPostDetail(userId, id));
    }

    // 10.3 Create Post
    @Operation(summary = "Create new post (Text + Image)")
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<ForumDTO.PostResponse> createPost(
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(forumService.createPost(getCurrentUserId(authentication), content, file));
    }

    // 10.4 Update Post
    @Operation(summary = "Update post (Text + Image)")
    @PutMapping(value = "/posts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<ForumDTO.PostResponse> updatePost(
            @PathVariable String id,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(forumService.updatePost(getCurrentUserId(authentication), id, content, file));
    }

    // 10.5 Delete Post
    @Operation(summary = "Delete post (Owner only)")
    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<String> deletePost(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        forumService.deletePost(getCurrentUserId(authentication), id);
        return ResponseEntity.ok("Post deleted successfully");
    }

    // 10.6 Toggle Like
    @Operation(summary = "Toggle Like/Unlike post")
    @PostMapping("/posts/{id}/like")
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<String> toggleLike(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        forumService.toggleLike(getCurrentUserId(authentication), id);
        return ResponseEntity.ok("Success");
    }

    // 10.7 Add Comment
    @Operation(summary = "Create a comment (Text + Image)")
    // 👇 Thêm postId vào URL cho rõ ràng bài viết nào
    @PostMapping(value = "/posts/{postId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<ForumDTO.CommentResponse> createComment(
            @PathVariable String postId,
            @ModelAttribute ForumDTO.CommentRequest request, // Dùng ModelAttribute để nhận file
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(forumService.createComment(
                getCurrentUserId(authentication),
                postId,
                request
        ));
    }

    @Operation(summary = "Update a comment (Text + Image)")
    // 👇 Thêm consumes multipart để nhận file
    @PutMapping(value = "/comments/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<ForumDTO.CommentResponse> updateComment(
            @PathVariable String id,
            @ModelAttribute ForumDTO.UpdateCommentRequest request, // 👇 Đổi @RequestBody thành @ModelAttribute
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(forumService.updateComment(
                getCurrentUserId(authentication), 
                id, 
                request // Truyền cả object request vào service
        ));
    }
    // 10.8 Delete Comment
    @Operation(summary = "Delete comment (Owner only)")
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'EXPERT', 'ADMIN')")
    public ResponseEntity<String> deleteComment(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        forumService.deleteComment(getCurrentUserId(authentication), id);
        return ResponseEntity.ok("Comment deleted successfully");
    }
}