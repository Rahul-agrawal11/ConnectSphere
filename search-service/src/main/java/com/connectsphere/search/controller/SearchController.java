package com.connectsphere.search.controller;

import com.connectsphere.search.dto.response.ApiResponse;
import com.connectsphere.search.dto.response.HashtagResponse;
import com.connectsphere.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Search REST controller.
 *
 * Exposes two groups of endpoints:
 *   /api/v1/search/**   — post and user search
 *   /api/v1/hashtags/** — hashtag indexing, trending and lookup
 *
 * All read endpoints are open (guests can search).
 * Indexing endpoints are internal (called by post-service).
 *
 * The controller depends only on SearchService (the interface).
 * Swapping the implementation for Elasticsearch requires
 * zero changes here.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Search", description = "Post/user search, hashtag indexing and trending")
public class SearchController {

    private final SearchService searchService;

    // ── Post Search ───────────────────────────────────────────────────────

    @Operation(
            summary = "Search posts by keyword",
            description = "Returns paginated post IDs matching the keyword. " +
                    "Full post data is fetched from post-service. " +
                    "Currently delegates to post-service's DB LIKE search."
    )
    @GetMapping("/api/v1/search/posts")
    public ResponseEntity<ApiResponse<Object>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Delegate entirely to post-service via Feign
        // Raw Object return avoids DTO coupling between services
        Object result = searchService.searchUsers(keyword);
        try {
            Pageable pageable = PageRequest.of(page, size);
            searchService.searchPostIds(keyword, pageable);
        } catch (Exception e) {
            log.warn("Post search delegation failed: {}", e.getMessage());
        }

        // Pass through post-service result directly
        return ResponseEntity.ok(
                ApiResponse.success("Post search results", result));
    }

    // ── User Search ───────────────────────────────────────────────────────

    @Operation(
            summary = "Search users by username or full name",
            description = "Delegates to auth-service user search. " +
                    "Returns list of user summaries."
    )
    @GetMapping("/api/v1/search/users")
    public ResponseEntity<ApiResponse<Object>> searchUsers(
            @RequestParam String query) {

        Object result = searchService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success("User search results", result));
    }

    // ── Hashtag Indexing (Internal — called by post-service) ──────────────

    @Operation(
            summary = "[Internal] Index hashtags from a post",
            description = "Called by post-service after creating or updating a post. " +
                    "Parses #tags from content and upserts Hashtag records."
    )
    @PostMapping("/api/v1/hashtags/index")
    public ResponseEntity<ApiResponse<Void>> indexPost(
            @RequestParam Long postId,
            @RequestParam String content) {

        searchService.indexPost(postId, content);
        return ResponseEntity.ok(
                ApiResponse.success("Post indexed successfully"));
    }

    @Operation(
            summary = "[Internal] Remove hashtag index for a deleted post",
            description = "Called by post-service when a post is deleted. " +
                    "Removes PostHashtag mappings and decrements postCount."
    )
    @DeleteMapping("/api/v1/hashtags/index/{postId}")
    public ResponseEntity<ApiResponse<Void>> removePostIndex(
            @PathVariable Long postId) {

        searchService.removePostIndex(postId);
        return ResponseEntity.ok(
                ApiResponse.success("Post index removed"));
    }

    // ── Hashtag Lookup ────────────────────────────────────────────────────

    @Operation(summary = "Get a hashtag by its tag name")
    @GetMapping("/api/v1/hashtags/{tag}")
    public ResponseEntity<ApiResponse<HashtagResponse>> getHashtag(
            @PathVariable String tag) {

        return ResponseEntity.ok(
                ApiResponse.success("Hashtag found",
                        searchService.getHashtagByTag(tag)));
    }

    @Operation(summary = "Get all hashtags for a specific post")
    @GetMapping("/api/v1/hashtags/post/{postId}")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> getHashtagsForPost(
            @PathVariable Long postId) {

        return ResponseEntity.ok(
                ApiResponse.success("Hashtags for post",
                        searchService.getHashtagsForPost(postId)));
    }

    // ── Trending Hashtags ─────────────────────────────────────────────────

    @Operation(
            summary = "Get trending hashtags",
            description = "Returns hashtags ordered by post count descending. " +
                    "Max 20 results. Available to all users including guests."
    )
    @GetMapping("/api/v1/hashtags/trending")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success("Trending hashtags",
                        searchService.getTrendingHashtags(limit)));
    }

    // ── Posts by Hashtag ──────────────────────────────────────────────────

    @Operation(
            summary = "Get post IDs for a hashtag (paginated)",
            description = "Returns IDs of posts tagged with the given hashtag. " +
                    "Use post-service to resolve IDs to full post data."
    )
    @GetMapping("/api/v1/hashtags/{tag}/posts")
    public ResponseEntity<ApiResponse<Page<Long>>> getPostsByHashtag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Posts for hashtag",
                        searchService.getPostIdsByHashtag(tag, pageable)));
    }

    // ── Hashtag Search (Autocomplete) ─────────────────────────────────────

    @Operation(
            summary = "Search hashtags by partial name",
            description = "Returns hashtags whose tag contains the query string. " +
                    "Ordered by postCount desc. Used for hashtag autocomplete."
    )
    @GetMapping("/api/v1/search/hashtags")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> searchHashtags(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success("Hashtag search results",
                        searchService.searchHashtags(query, limit)));
    }

    // ── Hashtag Post Count ────────────────────────────────────────────────

    @Operation(summary = "Get post count for a specific hashtag")
    @GetMapping("/api/v1/hashtags/{tag}/count")
    public ResponseEntity<ApiResponse<Long>> getHashtagCount(
            @PathVariable String tag) {

        return ResponseEntity.ok(
                ApiResponse.success("Hashtag post count",
                        searchService.getPostCountByHashtag(tag)));
    }
}