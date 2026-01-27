package com.nutrimate.service;

import com.nutrimate.dto.ForumDTO;
import com.nutrimate.entity.*;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    // 10.1 Lấy Newsfeed
    public Page<ForumDTO.PostResponse> getNewsFeed(String currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        List<ForumDTO.PostResponse> dtos = posts.stream().map(post -> {
            boolean isLiked = currentUserId != null && likeRepository.existsByUserIdAndPostId(currentUserId, post.getId());
            return mapToPostDTO(post, isLiked);
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, posts.getTotalElements());
    }

    // 10.2 Xem chi tiết bài viết + Comments
    public ForumDTO.PostDetailResponse getPostDetail(String currentUserId, String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        boolean isLiked = currentUserId != null && likeRepository.existsByUserIdAndPostId(currentUserId, postId);
        
        List<ForumDTO.CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream().map(this::mapToCommentDTO).collect(Collectors.toList());

        return ForumDTO.PostDetailResponse.builder()
                .post(mapToPostDTO(post, isLiked))
                .comments(comments)
                .build();
    }

    // 10.3 Đăng bài
    @Transactional
    public ForumDTO.PostResponse createPost(String userId, String content, MultipartFile file) {
        User user = userRepository.findById(userId).orElseThrow();
        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        
        // Xử lý upload ảnh nếu có
        if (file != null && !file.isEmpty()) {
            try {
                String url = fileUploadService.uploadFile(file);
                post.setImageUrl(url);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
            }
        }
        
        post.setLikeCount(0);
        post.setCommentCount(0);
        postRepository.save(post);
        return mapToPostDTO(post, false);
    }

    // 10.4 Sửa bài (Chỉ chủ bài viết)
    @Transactional
    public ForumDTO.PostResponse updatePost(String userId, String postId, String content, MultipartFile file) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        
        // Check quyền: Chỉ chủ bài viết mới được sửa
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You are not the author");
        }

        // Cập nhật nội dung text
        post.setContent(content);
        
        // Cập nhật ảnh (Nếu có gửi file mới lên thì thay thế ảnh cũ)
        if (file != null && !file.isEmpty()) {
            try {
                String url = fileUploadService.uploadFile(file);
                post.setImageUrl(url);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh cập nhật: " + e.getMessage());
            }
        }
        // Lưu ý: Nếu user không gửi file (file == null), ta giữ nguyên ảnh cũ (không xóa).

        post.setUpdatedAt(LocalDateTime.now());
        
        return mapToPostDTO(postRepository.save(post), likeRepository.existsByUserIdAndPostId(userId, postId));
    }

    // 10.5 Xóa bài (Chỉ chủ bài viết)
    @Transactional
    public void deletePost(String userId, String postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        postRepository.delete(post);
    }

    // 11.1 ADMIN: Xóa bài vi phạm (hard delete)
    @Transactional
    public void adminDeletePost(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        postRepository.delete(post);
    }

    // 11.2 ADMIN: Ẩn bài viết (soft delete - không xóa hẳn)
    @Transactional
    public ForumDTO.PostResponse adminHidePost(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Đánh dấu là đã bị ẩn bởi Admin bằng cách thay nội dung,
        // giữ lại record để không mất lịch sử / thống kê.
        post.setContent("[Hidden by admin due to policy violation]");
        post.setImageUrl(null);
        post.setUpdatedAt(LocalDateTime.now());

        Post saved = postRepository.save(post);
        // isLikedByCurrentUser không quan trọng trong ngữ cảnh Admin
        return mapToPostDTO(saved, false);
    }

    // 10.6 Thả tim / Bỏ tim (Toggle)
    @Transactional
    public void toggleLike(String userId, String postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        
        Optional<PostLike> existingLike = likeRepository.findByUserIdAndPostId(userId, postId);
        
        if (existingLike.isPresent()) {
            // Đã like -> Xóa like (Unlike)
            likeRepository.delete(existingLike.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            // Chưa like -> Tạo like
            User user = userRepository.findById(userId).orElseThrow();
            PostLike newLike = new PostLike();
            newLike.setPost(post);
            newLike.setUser(user);
            likeRepository.save(newLike);
            post.setLikeCount(post.getLikeCount() + 1);
        }
        postRepository.save(post);
    }

    // 10.7 Viết bình luận
    @Transactional
    public ForumDTO.CommentResponse addComment(String userId, String postId, String content, MultipartFile file) {
        User user = userRepository.findById(userId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        
        // Xử lý upload ảnh comment
        if (file != null && !file.isEmpty()) {
            try {
                String url = fileUploadService.uploadFile(file);
                comment.setImageUrl(url);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh comment: " + e.getMessage());
            }
        }
        
        commentRepository.save(comment);

        // Update count
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return mapToCommentDTO(comment);
    }

    // 10.8 Xóa bình luận (chỉ chủ comment)
    @Transactional
    public void deleteComment(String userId, String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        Post post = comment.getPost();
        commentRepository.delete(comment);
        
        // Update count
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    // 11.3 ADMIN: Xóa bình luận toxic / spam
    @Transactional
    public void adminDeleteComment(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Post post = comment.getPost();
        commentRepository.delete(comment);

        // Cập nhật lại số comment của bài viết
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    // Mappers
    private ForumDTO.PostResponse mapToPostDTO(Post post, boolean isLiked) {
        return ForumDTO.PostResponse.builder()
                .postId(post.getId())
                .authorName(post.getUser().getFullName())
                .authorAvatar(post.getUser().getAvatarUrl())
                .authorId(post.getUser().getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isLikedByCurrentUser(isLiked)
                .build();
    }

    private ForumDTO.CommentResponse mapToCommentDTO(Comment comment) {
        return ForumDTO.CommentResponse.builder()
                .commentId(comment.getId())
                .authorName(comment.getUser().getFullName())
                .authorAvatar(comment.getUser().getAvatarUrl())
                .authorId(comment.getUser().getId())
                .content(comment.getContent())
                .imageUrl(comment.getImageUrl())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}