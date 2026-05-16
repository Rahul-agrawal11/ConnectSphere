package com.connectsphere.search.client;

import com.connectsphere.search.dto.response.PostSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for post-service.
 *
 * Used to:
 *   1. Fetch post details by ID for search result hydration
 *   2. Search posts by keyword (delegating to post-service's own search)
 *
 * Why delegate post search to post-service instead of doing it here?
 *   - post-service owns the post content — single source of truth
 *   - search-service owns hashtag indexing and trending — its domain
 *   - Avoids content duplication in cs_search_db
 *   - When Elasticsearch is added, search-service indexes ES directly
 *     from the post content passed during indexPost()
 */
@FeignClient(name = "post-service", path = "/api/v1/posts")
public interface PostServiceClient {

    /**
     * Search posts by keyword — delegates to post-service's DB LIKE search.
     * Returns a list of post IDs from the search results.
     */
    @GetMapping("/search")
    Object searchPosts(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);

    /**
     * Fetch a single post's summary by ID.
     * Used to hydrate PostHashtag results into PostSummaryResponse.
     */
    @GetMapping("/{postId}")
    Object getPostById(@PathVariable("postId") Long postId);
}