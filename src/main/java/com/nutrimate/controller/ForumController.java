package com.nutrimate.controller;

import com.nutrimate.dto.ForumDTO;
import com.nutrimate.entity.User;
import com.nutrimate.repository.UserRepository;
import com.nutrimate.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
@Tag(name = "Community Forum", description = "Social features: Posts, Likes, Comments")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final UserRepository userRepository;

    private String getCurrentUserId(OidcUser oidcUser, OAuth2User oauth2User) {
        String email = (oidcUser != null) ? oidcUser.getEmail() : oauth2User.getAttribute("email");
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 10.1 GET /api/forum/posts (Newsfeed)
    @Operation(summary = "Get Newsfeed (List posts)")
    @GetMapping("/posts")
    public ResponseEntity<Page<ForumDTO.PostResponse>> getNewsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        // Có thể cho guest xem, nên userId có thể null nếu chưa login
        String userId = null;
        try { userId = getCurrentUserId(oidcUser, oauth2User); } catch (Exception ignored) {}

        return ResponseEntity.ok(forumService.getNewsFeed(userId, PageRequest.of(page, size)));
    }

    // 10.2 GET /api/forum/posts/{id} (Detail)
    @Operation(summary = "Get Post Detail + Comments")
    @GetMapping("/posts/{id}")
    public ResponseEntity<ForumDTO.PostDetailResponse> getPostDetail(
            @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        String userId = null;
        try { userId = getCurrentUserId(oidcUser, oauth2User); } catch (Exception ignored) {}
        
        return ResponseEntity.ok(forumService.getPostDetail(userId, id));
    }

    // 10.3 POST /api/forum/posts (Create)
    @Operation(summary = "Create new post (Text + Image)")
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 👈 Quan trọng
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ForumDTO.PostResponse> createPost(
            @RequestParam("content") String content, // Nhận text
            @RequestParam(value = "file", required = false) MultipartFile file, // Nhận file (không bắt buộc)
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        return ResponseEntity.ok(forumService.createPost(getCurrentUserId(oidcUser, oauth2User), content, file));
    }

    // 10.4 PUT /api/forum/posts/{id} (Update)
    @Operation(summary = "Update post (Text + Image)")
    @PutMapping(value = "/posts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 👈 Quan trọng
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ForumDTO.PostResponse> updatePost(
            @PathVariable String id,
            @RequestParam("content") String content, // Nhận text mới
            @RequestParam(value = "file", required = false) MultipartFile file, // Nhận file ảnh mới (không bắt buộc)
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        return ResponseEntity.ok(forumService.updatePost(getCurrentUserId(oidcUser, oauth2User), id, content, file));
    }

    // 10.5 DELETE /api/forum/posts/{id} (Delete)
    @Operation(summary = "Delete post (Owner only)")
    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> deletePost(
            @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        forumService.deletePost(getCurrentUserId(oidcUser, oauth2User), id);
        return ResponseEntity.ok("Post deleted successfully");
    }

    // 10.6 POST /api/forum/posts/{id}/like (Toggle Like)
    @Operation(summary = "Toggle Like/Unlike post")
    @PostMapping("/posts/{id}/like")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> toggleLike(
            @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        forumService.toggleLike(getCurrentUserId(oidcUser, oauth2User), id);
        return ResponseEntity.ok("Success");
    }

    // 10.7 POST /api/forum/posts/{id}/comments (Add Comment)
    @Operation(summary = "Add comment (Text + Image)")
    @PostMapping(value = "/posts/{id}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 👈 Quan trọng
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ForumDTO.CommentResponse> addComment(
            @PathVariable String id,
            @RequestParam("content") String content, // Nhận text
            @RequestParam(value = "file", required = false) MultipartFile file, // Nhận file
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        
        return ResponseEntity.ok(forumService.addComment(getCurrentUserId(oidcUser, oauth2User), id, content, file));
    }

    // 10.8 DELETE /api/forum/comments/{id} (Delete Comment)
    @Operation(summary = "Delete comment (Owner only)")
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> deleteComment(
            @PathVariable String id,
            @Parameter(hidden = true) @AuthenticationPrincipal OidcUser oidcUser,
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oauth2User) {
        forumService.deleteComment(getCurrentUserId(oidcUser, oauth2User), id);
        return ResponseEntity.ok("Comment deleted successfully");
    }
}