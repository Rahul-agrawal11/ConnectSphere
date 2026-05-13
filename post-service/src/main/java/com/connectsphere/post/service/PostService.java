package com.connectsphere.post.service;

import com.connectsphere.post.dto.request.CreatePostRequest;
import com.connectsphere.post.dto.request.UpdatePostRequest;
import com.connectsphere.post.dto.response.PostResponse;
import com.connectsphere.post.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Post service contract.
 */
public interface PostService {

    // CRUD
    PostResponse createPost(Long authorId, CreatePostRequest request);
    PostResponse getPostById(Long postId, Long requesterId);
    PostResponse updatePost(Long postId, Long requesterId, UpdatePostRequest request);
    void deletePost(Long postId, Long requesterId);

    // Feeds and listings
    Page<PostResponse> getPostsByUser(Long authorId, Long requesterId, Pageable pageable);
    Page<PostResponse> getPublicFeed(Pageable pageable);
    Page<PostResponse> getFeedForUser(List<Long> followedUserIds, Pageable pageable);

    // Search
    Page<PostResponse> searchPosts(String keyword, Pageable pageable);

    // Visibility
    PostResponse changeVisibility(Long postId, Long requesterId, PostVisibility visibility);

    // Counter endpoints — called by like-service and comment-service
    void incrementLikes(Long postId);
    void decrementLikes(Long postId);
    void incrementComments(Long postId);
    void decrementComments(Long postId);
    void incrementShares(Long postId);

    // Stats
    long getPostCount(Long authorId);

    // Owner lookup — called by like-service to resolve notification recipient
    Long getPostOwnerId(Long postId);

    // Admin
    void adminDeletePost(Long postId);

    // Admin — get all posts regardless of visibility
    Page<PostResponse> adminGetAllPosts(Pageable pageable);
}