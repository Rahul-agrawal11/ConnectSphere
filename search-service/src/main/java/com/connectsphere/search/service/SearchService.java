package com.connectsphere.search.service;

import com.connectsphere.search.dto.response.HashtagResponse;
import com.connectsphere.search.dto.response.PostSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Search service contract.
 *
 * This interface is the Elasticsearch migration boundary.
 *
 * Current implementation: DB-based (MySQL LIKE + JPA).
 * Future implementation: Elasticsearch-based.
 *
 * The controller only depends on this interface — swapping the
 * implementation class requires zero controller changes.
 */
public interface SearchService {

    // ── Indexing (called by post-service after post create/update/delete) ─

    /**
     * Index a post: extract hashtags from content, upsert Hashtag records,
     * create PostHashtag mappings.
     *
     * @param postId  the post's ID
     * @param content the full post content text (may contain #tags)
     */
    void indexPost(Long postId, String content);

    /**
     * Remove a post's index: delete PostHashtag rows, decrement
     * postCount on affected Hashtag records.
     *
     * @param postId the post to de-index
     */
    void removePostIndex(Long postId);

    // ── Post Search ───────────────────────────────────────────────────────

    /**
     * Full-text search across post content.
     * Returns page of post IDs — caller resolves to full posts via
     * post-service if needed.
     */
    Page<Long> searchPostIds(String keyword, Pageable pageable);

    // ── User Search ───────────────────────────────────────────────────────

    /**
     * Search users by username or full name.
     * Delegates to auth-service via Feign.
     * Returns raw response (Object) to avoid DTO coupling.
     */
    Object searchUsers(String query);

    // ── Hashtag Operations ────────────────────────────────────────────────

    List<HashtagResponse> getHashtagsForPost(Long postId);

    List<HashtagResponse> getTrendingHashtags(int limit);

    Page<Long> getPostIdsByHashtag(String tag, Pageable pageable);

    List<HashtagResponse> searchHashtags(String query, int limit);

    HashtagResponse getHashtagByTag(String tag);

    long getPostCountByHashtag(String tag);
}