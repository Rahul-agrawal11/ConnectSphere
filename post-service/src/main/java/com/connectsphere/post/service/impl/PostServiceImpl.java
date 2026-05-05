package com.connectsphere.post.service.impl;

import com.connectsphere.post.dto.request.CreatePostRequest;
import com.connectsphere.post.dto.request.UpdatePostRequest;
import com.connectsphere.post.dto.response.PostResponse;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.enums.PostVisibility;
import com.connectsphere.post.exception.PostNotFoundException;
import com.connectsphere.post.exception.UnauthorizedActionException;
import com.connectsphere.post.repository.PostRepository;
import com.connectsphere.post.service.PostService;
import com.connectsphere.post.client.SearchServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    private final SearchServiceClient searchServiceClient;

    // ── CRUD ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PostResponse createPost(Long authorId, CreatePostRequest request) {
        Post post = Post.builder()
                .authorId(authorId)
                .content(request.getContent())
                .visibility(request.getVisibility())
                .build();

        post.setMediaUrlList(request.getMediaUrls());

        Post saved = postRepository.save(post);

        try {
            searchServiceClient.indexPost(saved.getId(), saved.getContent());
        } catch (Exception e) {
            log.warn("Failed to index hashtags for postId={}: {}", saved.getId(), e.getMessage());
        }

        log.info("Post created: id={} by authorId={}", saved.getId(), authorId);
        return mapToResponse(saved);
    }

    @Override
    public PostResponse getPostById(Long postId, Long requesterId) {
        Post post = findActivePost(postId);
        enforceReadAccess(post, requesterId);
        return mapToResponse(post);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long postId, Long requesterId,
                                   UpdatePostRequest request) {
        Post post = findActivePost(postId);
        enforceOwnership(post, requesterId);

        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getMediaUrls() != null) {
            post.setMediaUrlList(request.getMediaUrls());
        }
        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }

        Post saved = postRepository.save(post);

        try {
            searchServiceClient.removePostIndex(saved.getId());
            searchServiceClient.indexPost(saved.getId(), saved.getContent());
        } catch (Exception e) {
            log.warn("Failed to re-index hashtags for postId={}: {}", saved.getId(), e.getMessage());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long requesterId) {
        Post post = findActivePost(postId);
        enforceOwnership(post, requesterId);

        post.setIsDeleted(true);
        postRepository.save(post);

        try {
            searchServiceClient.removePostIndex(postId);
        } catch (Exception e) {
            log.warn("Failed to remove hashtag index for postId={}: {}", postId, e.getMessage());
        }

        log.info("Post soft-deleted: id={} by requesterId={}", postId, requesterId);
    }

    // ── Feeds and Listings ──────────────────────────────────────────────

    @Override
    public Page<PostResponse> getPostsByUser(Long authorId, Long requesterId,
                                             Pageable pageable) {
        // If requester is the author → show all their posts
        if (authorId.equals(requesterId)) {
            return postRepository
                    .findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(
                            authorId, pageable)
                    .map(this::mapToResponse);
        }

        // Otherwise → show only PUBLIC posts from that author
        return postRepository
                .findByAuthorIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                        authorId, PostVisibility.PUBLIC, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<PostResponse> getPublicFeed(Pageable pageable) {
        return postRepository
                .findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                        PostVisibility.PUBLIC, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<PostResponse> getFeedForUser(List<Long> followedUserIds,
                                             Pageable pageable) {
        if (followedUserIds == null || followedUserIds.isEmpty()) {
            // Fall back to public feed when user follows nobody
            return getPublicFeed(pageable);
        }
        return postRepository
                .findFeedByAuthorIds(followedUserIds, pageable)
                .map(this::mapToResponse);
    }

    // ── Search ──────────────────────────────────────────────────────────

    @Override
    public Page<PostResponse> searchPosts(String keyword, Pageable pageable) {
        return postRepository
                .searchByContent(keyword, pageable)
                .map(this::mapToResponse);
    }

    // ── Visibility ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PostResponse changeVisibility(Long postId, Long requesterId,
                                         PostVisibility visibility) {
        Post post = findActivePost(postId);
        enforceOwnership(post, requesterId);
        post.setVisibility(visibility);
        return mapToResponse(postRepository.save(post));
    }

    // ── Counter Endpoints ───────────────────────────────────────────────

    @Override
    @Transactional
    public void incrementLikes(Long postId) {
        int updated = postRepository.incrementLikesCount(postId);
        if (updated == 0) throw new PostNotFoundException("Post not found: " + postId);
    }

    @Override
    @Transactional
    public void decrementLikes(Long postId) {
        int updated = postRepository.decrementLikesCount(postId);
        if (updated == 0) throw new PostNotFoundException("Post not found: " + postId);
    }

    @Override
    @Transactional
    public void incrementComments(Long postId) {
        int updated = postRepository.incrementCommentsCount(postId);
        if (updated == 0) throw new PostNotFoundException("Post not found: " + postId);
    }

    @Override
    @Transactional
    public void decrementComments(Long postId) {
        int updated = postRepository.decrementCommentsCount(postId);
        if (updated == 0) throw new PostNotFoundException("Post not found: " + postId);
    }

    @Override
    @Transactional
    public void incrementShares(Long postId) {
        int updated = postRepository.incrementSharesCount(postId);
        if (updated == 0) throw new PostNotFoundException("Post not found: " + postId);
    }

    // ── Stats ────────────────────────────────────────────────────────────

    @Override
    public long getPostCount(Long authorId) {
        return postRepository.countByAuthorIdAndIsDeletedFalse(authorId);
    }

    // ── Admin ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void adminDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(
                        "Post not found: " + postId));
        post.setIsDeleted(true);
        postRepository.save(post);
        log.info("Post force-deleted by admin: id={}", postId);
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    private Post findActivePost(Long postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new PostNotFoundException(
                        "Post not found or has been deleted: " + postId));
    }

    /**
     * Enforce that the requester is the post author.
     * Throws UnauthorizedActionException if not.
     */
    private void enforceOwnership(Post post, Long requesterId) {
        if (!post.getAuthorId().equals(requesterId)) {
            throw new UnauthorizedActionException(
                    "You are not allowed to modify this post.");
        }
    }

    /**
     * Enforce read access based on visibility.
     * PRIVATE posts are only visible to the author.
     * FOLLOWERS_ONLY — the gateway/follow-service decides; we allow here
     * and rely on the feed query to filter correctly.
     */
    private void enforceReadAccess(Post post, Long requesterId) {
        if (post.getVisibility() == PostVisibility.PRIVATE
                && !post.getAuthorId().equals(requesterId)) {
            throw new UnauthorizedActionException(
                    "This post is private.");
        }
    }

    /**
     * Map entity to response DTO.
     */
    private PostResponse mapToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .mediaUrls(post.getMediaUrlList())
                .postType(post.getPostType().name())
                .visibility(post.getVisibility().name())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}