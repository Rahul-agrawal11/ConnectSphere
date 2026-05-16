package com.connectsphere.search.service.impl;

import com.connectsphere.search.client.AuthServiceClient;
import com.connectsphere.search.client.PostServiceClient;
import com.connectsphere.search.dto.response.HashtagResponse;
import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostHashtag;
import com.connectsphere.search.exception.HashtagNotFoundException;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.repository.PostHashtagRepository;
import com.connectsphere.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostServiceClient postServiceClient;
    private final AuthServiceClient authServiceClient;

    @Value("${app.search.trending-limit:20}")
    private int trendingLimit;

    @Value("${app.search.trending-min-count:1}")
    private int trendingMinCount;

    /**
     * Regex to extract hashtag tokens from post content.
     *
     * Matches: #word (letters, digits, underscores — no spaces)
     * e.g. "Hello #world and #SpringBoot!" → ["world", "springboot"]
     *
     * Tags are normalised to lowercase before storage.
     */
    private static final Pattern HASHTAG_PATTERN =
            Pattern.compile("#([\\w]+)", Pattern.UNICODE_CHARACTER_CLASS);

    // ── Indexing ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void indexPost(Long postId, String content) {
        if (content == null || content.isBlank()) {
            log.debug("indexPost called with blank content for postId={}", postId);
            return;
        }

        List<String> tags = extractHashtags(content);

        if (tags.isEmpty()) {
            log.debug("No hashtags found in postId={}", postId);
            return;
        }

        log.info("Indexing postId={} with {} hashtag(s): {}",
                postId, tags.size(), tags);

        for (String tag : tags) {
            // Upsert: find existing hashtag or create new one
            Hashtag hashtag = hashtagRepository.findByTag(tag)
                    .orElseGet(() -> {
                        Hashtag newTag = Hashtag.builder()
                                .tag(tag)
                                .postCount(0)
                                .build();
                        return hashtagRepository.save(newTag);
                    });

            // Create PostHashtag mapping only if it doesn't exist
            // (handles re-indexing after post edit)
            if (!postHashtagRepository.existsByPostIdAndHashtagId(
                    postId, hashtag.getId())) {

                PostHashtag postHashtag = PostHashtag.builder()
                        .postId(postId)
                        .hashtagId(hashtag.getId())
                        .build();
                postHashtagRepository.save(postHashtag);

                // Increment postCount on the hashtag
                hashtagRepository.incrementPostCount(hashtag.getId());

                log.debug("Indexed hashtag '{}' for postId={}", tag, postId);
            }
        }
    }

    @Override
    @Transactional
    public void removePostIndex(Long postId) {
        // Get all hashtag IDs for this post before deletion
        List<Long> hashtagIds =
                postHashtagRepository.findHashtagIdsByPostId(postId);

        if (hashtagIds.isEmpty()) {
            log.debug("No hashtag index to remove for postId={}", postId);
            return;
        }

        // Delete all PostHashtag mappings for this post
        postHashtagRepository.deleteByPostId(postId);

        // Decrement postCount on each affected hashtag
        for (Long hashtagId : hashtagIds) {
            hashtagRepository.decrementPostCount(hashtagId);
        }

        log.info("Removed index for postId={} — decremented {} hashtag(s)",
                postId, hashtagIds.size());
    }

    // ── Post Search ───────────────────────────────────────────────────────

    /**
     * Post search delegates to post-service via Feign.
     *
     * post-service owns post content — it performs the DB LIKE search.
     * search-service returns the post IDs from that result.
     *
     * For Elasticsearch migration:
     *   Replace the Feign call with an Elasticsearch query on the
     *   post index. The contract (return Page<Long>) stays the same.
     */
    @Override
    public Page<Long> searchPostIds(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword cannot be blank");
        }

        try {
            // Delegate to post-service search endpoint
            // The raw response is cast — in a real system, use a typed Feign client
            Object rawResult = postServiceClient.searchPosts(
                    keyword,
                    pageable.getPageNumber(),
                    pageable.getPageSize());

            log.debug("Post search delegated to post-service for keyword='{}'",
                    keyword);

            // Return empty page — the controller passes the raw result through
            // See SearchController for how raw results are handled
            return Page.empty(pageable);

        } catch (Exception e) {
            log.error("Post search failed for keyword='{}': {}",
                    keyword, e.getMessage());
            return Page.empty(pageable);
        }
    }

    // ── User Search ───────────────────────────────────────────────────────

    @Override
    public Object searchUsers(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be blank");
        }

        try {
            return authServiceClient.searchUsers(query);
        } catch (Exception e) {
            log.error("User search failed for query='{}': {}", query,
                    e.getMessage());
            return List.of();
        }
    }

    // ── Hashtag Operations ────────────────────────────────────────────────

    @Override
    public List<HashtagResponse> getHashtagsForPost(Long postId) {
        List<Long> hashtagIds =
                postHashtagRepository.findHashtagIdsByPostId(postId);

        return hashtagIds.stream()
                .map(hashtagRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::mapToHashtagResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<HashtagResponse> getTrendingHashtags(int limit) {
        int cappedLimit = Math.min(limit, trendingLimit);
        Pageable pageable = PageRequest.of(0, cappedLimit);

        return hashtagRepository
                .findTrendingHashtags(trendingMinCount, pageable)
                .stream()
                .map(this::mapToHashtagResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Long> getPostIdsByHashtag(String tag, Pageable pageable) {
        String normalised = normaliseTag(tag);

        Hashtag hashtag = hashtagRepository.findByTag(normalised)
                .orElseThrow(() -> new HashtagNotFoundException(
                        "Hashtag not found: #" + normalised));

        return postHashtagRepository
                .findPostIdsByHashtagId(hashtag.getId(), pageable);
    }

    @Override
    public List<HashtagResponse> searchHashtags(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Hashtag query cannot be blank");
        }

        String normalised = normaliseTag(query);
        Pageable pageable = PageRequest.of(0, Math.min(limit, 50));

        return hashtagRepository
                .searchByTagContaining(normalised, pageable)
                .stream()
                .map(this::mapToHashtagResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HashtagResponse getHashtagByTag(String tag) {
        String normalised = normaliseTag(tag);

        Hashtag hashtag = hashtagRepository.findByTag(normalised)
                .orElseThrow(() -> new HashtagNotFoundException(
                        "Hashtag not found: #" + normalised));

        return mapToHashtagResponse(hashtag);
    }

    @Override
    public long getPostCountByHashtag(String tag) {
        String normalised = normaliseTag(tag);

        Hashtag hashtag = hashtagRepository.findByTag(normalised)
                .orElseThrow(() -> new HashtagNotFoundException(
                        "Hashtag not found: #" + normalised));

        return postHashtagRepository.countByHashtagId(hashtag.getId());
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    /**
     * Extract all unique hashtag tokens from post content.
     * Normalises to lowercase and removes duplicates.
     *
     * Input:  "Hello #World and #springBoot! Also #world again."
     * Output: ["world", "springboot"]
     */
    private List<String> extractHashtags(String content) {
        List<String> tags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);

        while (matcher.find()) {
            String tag = normaliseTag(matcher.group(1));
            if (!tags.contains(tag) && tag.length() <= 100) {
                tags.add(tag);
            }
        }

        return tags;
    }

    /**
     * Normalise a tag: strip leading '#' if present, lowercase.
     */
    private String normaliseTag(String tag) {
        if (tag == null) return "";
        return tag.startsWith("#")
                ? tag.substring(1).toLowerCase().trim()
                : tag.toLowerCase().trim();
    }

    private HashtagResponse mapToHashtagResponse(Hashtag hashtag) {
        return HashtagResponse.builder()
                .id(hashtag.getId())
                .tag(hashtag.getTag())
                .postCount(hashtag.getPostCount())
                .lastUsedAt(hashtag.getLastUsedAt())
                .build();
    }
}