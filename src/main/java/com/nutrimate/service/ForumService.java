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
    public ForumDTO.PostResponse createPost(String userId, ForumDTO.PostRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        Post post = new Post();
        post.setUser(user);
        post.setContent(req.getContent());
        post.setImageUrl(req.getImageUrl());
        post.setLikeCount(0);
        post.setCommentCount(0);
        
        postRepository.save(post);
        return mapToPostDTO(post, false);
    }

    // 10.4 Sửa bài (Chỉ chủ bài viết)
    @Transactional
    public ForumDTO.PostResponse updatePost(String userId, String postId, ForumDTO.PostRequest req) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You are not the author");
        }

        post.setContent(req.getContent());
        // Có thể cho sửa ảnh hoặc không tùy logic
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
    public ForumDTO.CommentResponse addComment(String userId, String postId, ForumDTO.CommentRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(req.getContent());
        commentRepository.save(comment);

        // Update count
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return mapToCommentDTO(comment);
    }

    // 10.8 Xóa bình luận
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
                .createdAt(comment.getCreatedAt())
                .build();
    }
}