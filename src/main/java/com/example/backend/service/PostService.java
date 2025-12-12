package com.example.backend.service;

import com.example.backend.dto.common.PageResponse;
import com.example.backend.dto.post.PostCreateRequest;
import com.example.backend.dto.post.PostResponse;
import com.example.backend.dto.post.PostUpdateRequest;
import com.example.backend.entity.Post;
import com.example.backend.entity.PostStats;
import com.example.backend.entity.User;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.PostStatsRepository;
import com.example.backend.service.mapper.EntityDtoMapper;
import com.example.backend.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostStatsRepository postStatsRepository;
    private final AuthService authService;
    private final HtmlSanitizer htmlSanitizer;
    private final EntityDtoMapper entityDtoMapper;

    @CacheEvict(value = "postList", allEntries = true)
    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        User currentUser = authService.getCurrentUser();

        String sanitizedTitle = htmlSanitizer.sanitize(request.getTitle());
        String sanitizedContent = htmlSanitizer.sanitize(request.getContent());

        Post post = Post.builder()
                .user(currentUser)
                .postTitle(sanitizedTitle)
                .postContents(sanitizedContent)
                .build();

        Post savedPost = postRepository.save(post);

        PostStats stats = PostStats.builder()
                .post(savedPost)
                .viewCount(0L)
                .likeCount(0L)
                .build();
        postStatsRepository.save(stats);

        return entityDtoMapper.toPostResponse(savedPost);
    }

    @Cacheable(value = "postDetail", key = "#postId")
    @Transactional
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        PostStats stats = postStatsRepository.findByPostId(postId)
                .orElseGet(() -> {
                    PostStats newStats = PostStats.builder()
                            .post(post)
                            .viewCount(0L)
                            .likeCount(0L)
                            .build();
                    return postStatsRepository.save(newStats);
                });

        stats.incrementViewCount();
        postStatsRepository.save(stats);

        return entityDtoMapper.toPostResponse(post);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getAllPosts(Pageable pageable) {
        Page<Post> postsPage = postRepository.findAllActive(pageable);
        return PageResponse.of(postsPage, entityDtoMapper::toPostResponse);
    }

    @Caching(evict = {
            @CacheEvict(value = "postDetail", key = "#postId"),
            @CacheEvict(value = "postList", allEntries = true)
    })
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User currentUser = authService.getCurrentUser();

        if (!post.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("게시글을 수정할 권한이 없습니다.");
        }

        String sanitizedTitle = htmlSanitizer.sanitize(request.getTitle());
        String sanitizedContent = htmlSanitizer.sanitize(request.getContent());

        post.setPostTitle(sanitizedTitle);
        post.setPostContents(sanitizedContent);

        Post updatedPost = postRepository.save(post);
        return entityDtoMapper.toPostResponse(updatedPost);
    }

    @Caching(evict = {
            @CacheEvict(value = "postDetail", key = "#postId"),
            @CacheEvict(value = "postList", allEntries = true)
    })
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User currentUser = authService.getCurrentUser();

        if (!post.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("게시글을 삭제할 권한이 없습니다.");
        }

        post.delete();
        postRepository.save(post);
    }
}