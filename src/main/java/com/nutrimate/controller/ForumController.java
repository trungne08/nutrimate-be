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
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum")
@Tag(name = "Community Forum", description = "Social features: Posts, Likes, Comments")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final UserRepository userRepository;

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null) {
             throw new RuntimeException("Unauthorized: User is not authenticated");
        }

        String email = null;
        Object principal = authentication.getPrincipal();

        // Trường hợp 1: Dùng Bearer Token (Postman / Mobile App)
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            // Cognito lưu email trong claim "email"
            email = jwt.getClaimAsString("email"); 
        } 
        // Trường hợp 2: Dùng Login Session (Trình duyệt)
        else if (principal instanceof OidcUser) {
            email = ((OidcUser) principal).getEmail();
        } 
        else if (principal instanceof OAuth2User) {
            email = ((OAuth2User) principal).getAttribute("email");
        }

        if (email == null) {
            throw new RuntimeException("Email not found in authentication token");
        }

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found in database"));
    }

    @Operation(summary = "Get Newsfeed (List posts)")
    @GetMapping("/posts")
    public ResponseEntity<Page<ForumDTO.PostResponse>> getNewsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) Authentication authentication) {
        
        String userId = null;
        try { userId = getCurrentUserId(authentication); } catch (Exception ignored) {}

        return ResponseEntity.ok(forumService.getNewsFeed(userId, PageRequest.of(page, size)));
    }

    @Operation(summary = "Get Post Detail + Comments")
    @GetMapping("/posts/{id}")
    public ResponseEntity<ForumDTO.PostDetailResponse> getPostDetail(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        String userId = null;
        try { userId = getCurrentUserId(authentication); } catch (Exception ignored) {}
        
        return ResponseEntity.ok(forumService.getPostDetail(userId, id));
    }

    @Operation(summary = "Create new post (Text + Image)")
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ForumDTO.PostResponse> createPost(
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(forumService.createPost(getCurrentUserId(authentication), content, file));
    }

    @Operation(summary = "Update post (Text + Image)")
    @PutMapping(value = "/posts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ForumDTO.PostResponse> updatePost(
            @PathVariable String id,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(forumService.updatePost(getCurrentUserId(authentication), id, content, file));
    }

    @Operation(summary = "Delete post (Owner only)")
    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> deletePost(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        forumService.deletePost(getCurrentUserId(authentication), id);
        return ResponseEntity.ok("Post deleted successfully");
    }

    @Operation(summary = "Toggle Like/Unlike post")
    @PostMapping("/posts/{id}/like")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> toggleLike(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        forumService.toggleLike(getCurrentUserId(authentication), id);
        return ResponseEntity.ok("Success");
    }

    @Operation(summary = "Add comment (Text + Image)")
    @PostMapping(value = "/posts/{id}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ForumDTO.CommentResponse> addComment(
            @PathVariable String id,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(hidden = true) Authentication authentication) {
        
        return ResponseEntity.ok(forumService.addComment(getCurrentUserId(authentication), id, content, file));
    }

    @Operation(summary = "Delete comment (Owner only)")
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<String> deleteComment(
            @PathVariable String id,
            @Parameter(hidden = true) Authentication authentication) {
        
        forumService.deleteComment(getCurrentUserId(authentication), id);
        return ResponseEntity.ok("Comment deleted successfully");
    }
}