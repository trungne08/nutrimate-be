package com.nutrimate.service;

import com.nutrimate.dto.ForumDTO;
import com.nutrimate.entity.*;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.exception.AccessDeniedException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.service.FileUploadService;
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

    // 10.1 Get Newsfeed
    public Page<ForumDTO.PostResponse> getNewsFeed(String currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        List<ForumDTO.PostResponse> dtos = posts.stream().map(post -> {
            boolean isLiked = currentUserId != null && likeRepository.existsByUserIdAndPostId(currentUserId, post.getId());
            return mapToPostDTO(post, isLiked);
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, posts.getTotalElements());
    }

    // 10.2 Get Post Detail
    public ForumDTO.PostDetailResponse getPostDetail(String currentUserId, String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found (ID: " + postId + ")"));
        
        boolean isLiked = currentUserId != null && likeRepository.existsByUserIdAndPostId(currentUserId, postId);
        
        List<ForumDTO.CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream().map(this::mapToCommentDTO).collect(Collectors.toList());

        return ForumDTO.PostDetailResponse.builder()
                .post(mapToPostDTO(post, isLiked))
                .comments(comments)
                .build();
    }

    // 10.3 Create Post
    @Transactional
    public ForumDTO.PostResponse createPost(String userId, String content, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        
        if (file != null && !file.isEmpty()) {
            try {
                String url = fileUploadService.uploadFile(file);
                post.setImageUrl(url);
            } catch (IOException e) {
                throw new BadRequestException("Image upload failed: " + e.getMessage());
            }
        }
        
        post.setLikeCount(0);
        post.setCommentCount(0);
        postRepository.save(post);
        return mapToPostDTO(post, false);
    }

    // 10.4 Update Post
    @Transactional
    public ForumDTO.PostResponse updatePost(String userId, String postId, String content, MultipartFile file) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to edit this post");
        }

        post.setContent(content);
        
        if (file != null && !file.isEmpty()) {
            try {
                String url = fileUploadService.uploadFile(file);
                post.setImageUrl(url);
            } catch (IOException e) {
                throw new BadRequestException("Image upload failed during update: " + e.getMessage());
            }
        }

        post.setUpdatedAt(LocalDateTime.now());
        
        return mapToPostDTO(postRepository.save(post), likeRepository.existsByUserIdAndPostId(userId, postId));
    }

    // 10.5 Delete Post
    @Transactional
    public void deletePost(String userId, String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to delete this post");
        }
        
        postRepository.delete(post);
    }

    // 11.1 ADMIN: Delete Post
    @Transactional
    public void adminDeletePost(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        postRepository.delete(post);
    }

    // 11.2 ADMIN: Hide Post
    @Transactional
    public ForumDTO.PostResponse adminHidePost(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setContent("[Content hidden by Admin due to community guidelines violation]");
        post.setImageUrl(null);
        post.setUpdatedAt(LocalDateTime.now());

        return mapToPostDTO(postRepository.save(post), false);
    }

    // 10.6 Toggle Like
    @Transactional
    public void toggleLike(String userId, String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        
        Optional<PostLike> existingLike = likeRepository.findByUserIdAndPostId(userId, postId);
        
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            PostLike newLike = new PostLike();
            newLike.setPost(post);
            newLike.setUser(user);
            likeRepository.save(newLike);
            post.setLikeCount(post.getLikeCount() + 1);
        }
        postRepository.save(post);
    }

    // 10.7 Add Comment
    @Transactional
    public ForumDTO.CommentResponse createComment(String userId, String postId, ForumDTO.CommentRequest request) {
        // 1. Tìm User và Post
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // 2. Tạo Entity Comment
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post);
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now()); // Nếu entity không tự gen

        // 3. XỬ LÝ ẢNH (Nếu có upload)
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            try {
                String imageUrl = fileUploadService.uploadFile(request.getFile());
                comment.setImageUrl(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh comment: " + e.getMessage());
            }
        }

        // 4. Lưu và trả về
        Comment savedComment = commentRepository.save(comment);
        return mapToCommentDTO(savedComment);
    }

    @Transactional
    public ForumDTO.CommentResponse updateComment(String userId, String commentId, ForumDTO.UpdateCommentRequest request) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận"));


        if (!comment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa bình luận này");
        }
        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }
        MultipartFile file = request.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                String newImageUrl = fileUploadService.uploadFile(file);
                comment.setImageUrl(newImageUrl); 
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh comment: " + e.getMessage());
            }
        }

        // 5. Lưu và trả về
        Comment savedComment = commentRepository.save(comment);
        return mapToCommentDTO(savedComment);
    }

    // 10.8 Delete Comment
    @Transactional
    public void deleteComment(String userId, String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to delete this comment");
        }
        
        Post post = comment.getPost();
        commentRepository.delete(comment);
        
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    // 11.3 ADMIN: Delete Comment
    @Transactional
    public void adminDeleteComment(String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        Post post = comment.getPost();
        commentRepository.delete(comment);

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
                .authorRole(post.getUser().getRole() != null ? post.getUser().getRole().name() : "MEMBER")
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
                .authorRole(comment.getUser().getRole() != null ? comment.getUser().getRole().name() : "MEMBER")
                .content(comment.getContent())
                .imageUrl(comment.getImageUrl())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}