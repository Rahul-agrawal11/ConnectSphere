package com.connectsphere.search.service;

import com.connectsphere.search.client.AuthServiceClient;
import com.connectsphere.search.client.PostServiceClient;
import com.connectsphere.search.dto.response.HashtagResponse;
import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.exception.HashtagNotFoundException;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.repository.PostHashtagRepository;
import com.connectsphere.search.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private PostHashtagRepository postHashtagRepository;

    @Mock
    private PostServiceClient postServiceClient;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private SearchServiceImpl searchService;

    private Hashtag hashtag;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(searchService, "trendingLimit", 20);
        ReflectionTestUtils.setField(searchService, "trendingMinCount", 1);

        hashtag = Hashtag.builder()
                .id(1L)
                .tag("springboot")
                .postCount(5)
                .lastUsedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void indexPost_ShouldDoNothing_WhenContentIsBlank() {
        searchService.indexPost(100L, "   ");

        verifyNoInteractions(hashtagRepository, postHashtagRepository);
    }

    @Test
    void indexPost_ShouldCreateNewHashtagAndMapping() {
        when(hashtagRepository.findByTag("springboot")).thenReturn(Optional.empty());
        when(hashtagRepository.save(any(Hashtag.class))).thenReturn(hashtag);
        when(postHashtagRepository.existsByPostIdAndHashtagId(100L, 1L)).thenReturn(false);

        searchService.indexPost(100L, "Learning #SpringBoot today");

        verify(hashtagRepository).save(any(Hashtag.class));
        verify(postHashtagRepository).save(any());
        verify(hashtagRepository).incrementPostCount(1L);
    }

    @Test
    void indexPost_ShouldNotDuplicateSameHashtagInSameContent() {
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.of(
                Hashtag.builder().id(2L).tag("java").postCount(1).build()
        ));
        when(postHashtagRepository.existsByPostIdAndHashtagId(100L, 2L)).thenReturn(false);

        searchService.indexPost(100L, "#Java #java #JAVA");

        verify(postHashtagRepository, times(1)).save(any());
        verify(hashtagRepository, times(1)).incrementPostCount(2L);
    }

    @Test
    void indexPost_ShouldNotIncrement_WhenMappingAlreadyExists() {
        when(hashtagRepository.findByTag("java")).thenReturn(Optional.of(
                Hashtag.builder().id(2L).tag("java").postCount(1).build()
        ));
        when(postHashtagRepository.existsByPostIdAndHashtagId(100L, 2L)).thenReturn(true);

        searchService.indexPost(100L, "#java");

        verify(postHashtagRepository, never()).save(any());
        verify(hashtagRepository, never()).incrementPostCount(anyLong());
    }

    @Test
    void removePostIndex_ShouldDeleteMappingsAndDecrementCounts() {
        when(postHashtagRepository.findHashtagIdsByPostId(100L)).thenReturn(List.of(1L, 2L));

        searchService.removePostIndex(100L);

        verify(postHashtagRepository).deleteByPostId(100L);
        verify(hashtagRepository).decrementPostCount(1L);
        verify(hashtagRepository).decrementPostCount(2L);
    }

    @Test
    void removePostIndex_ShouldDoNothing_WhenNoHashtagsFound() {
        when(postHashtagRepository.findHashtagIdsByPostId(100L)).thenReturn(List.of());

        searchService.removePostIndex(100L);

        verify(postHashtagRepository, never()).deleteByPostId(anyLong());
        verify(hashtagRepository, never()).decrementPostCount(anyLong());
    }

    @Test
    void searchPostIds_ShouldCallPostServiceAndReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(postServiceClient.searchPosts("java", 0, 10)).thenReturn(Map.of("success", true));

        Page<Long> result = searchService.searchPostIds("java", pageable);

        assertTrue(result.isEmpty());
        verify(postServiceClient).searchPosts("java", 0, 10);
    }

    @Test
    void searchPostIds_ShouldThrowException_WhenKeywordBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> searchService.searchPostIds(" ", PageRequest.of(0, 10)));
    }

    @Test
    void searchPostIds_ShouldReturnEmptyPage_WhenPostServiceFails() {
        Pageable pageable = PageRequest.of(0, 10);

        when(postServiceClient.searchPosts("java", 0, 10))
                .thenThrow(new RuntimeException("post-service down"));

        Page<Long> result = searchService.searchPostIds("java", pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchUsers_ShouldReturnAuthServiceResult() {
        Object users = List.of(Map.of("username", "rahul"));

        when(authServiceClient.searchUsers("rahul")).thenReturn(users);

        Object result = searchService.searchUsers("rahul");

        assertEquals(users, result);
    }

    @Test
    void searchUsers_ShouldThrowException_WhenQueryBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> searchService.searchUsers(""));
    }

    @Test
    void searchUsers_ShouldReturnEmptyList_WhenAuthServiceFails() {
        when(authServiceClient.searchUsers("rahul"))
                .thenThrow(new RuntimeException("auth-service down"));

        Object result = searchService.searchUsers("rahul");

        assertEquals(List.of(), result);
    }

    @Test
    void getHashtagsForPost_ShouldReturnHashtags() {
        when(postHashtagRepository.findHashtagIdsByPostId(100L)).thenReturn(List.of(1L));
        when(hashtagRepository.findById(1L)).thenReturn(Optional.of(hashtag));

        List<HashtagResponse> result = searchService.getHashtagsForPost(100L);

        assertEquals(1, result.size());
        assertEquals("springboot", result.get(0).getTag());
    }

    @Test
    void getTrendingHashtags_ShouldReturnTrendingTags() {
        Pageable pageable = PageRequest.of(0, 10);

        when(hashtagRepository.findTrendingHashtags(eq(1), any(Pageable.class)))
                .thenReturn(List.of(hashtag));

        List<HashtagResponse> result = searchService.getTrendingHashtags(10);

        assertEquals(1, result.size());
        assertEquals("springboot", result.get(0).getTag());
    }

    @Test
    void getTrendingHashtags_ShouldCapLimitAtConfiguredLimit() {
        when(hashtagRepository.findTrendingHashtags(eq(1), any(Pageable.class)))
                .thenReturn(List.of(hashtag));

        searchService.getTrendingHashtags(100);

        verify(hashtagRepository).findTrendingHashtags(eq(1), argThat(pageable ->
                pageable.getPageSize() == 20
        ));
    }

    @Test
    void getPostIdsByHashtag_ShouldReturnPostIds() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> page = new PageImpl<>(List.of(100L, 101L), pageable, 2);

        when(hashtagRepository.findByTag("springboot")).thenReturn(Optional.of(hashtag));
        when(postHashtagRepository.findPostIdsByHashtagId(1L, pageable)).thenReturn(page);

        Page<Long> result = searchService.getPostIdsByHashtag("#SpringBoot", pageable);

        assertEquals(2, result.getContent().size());
    }

    @Test
    void getPostIdsByHashtag_ShouldThrowException_WhenHashtagMissing() {
        when(hashtagRepository.findByTag("unknown")).thenReturn(Optional.empty());

        assertThrows(HashtagNotFoundException.class,
                () -> searchService.getPostIdsByHashtag("unknown", PageRequest.of(0, 10)));
    }

    @Test
    void searchHashtags_ShouldReturnMatchingHashtags() {
        when(hashtagRepository.searchByTagContaining(eq("spring"), any(Pageable.class)))
                .thenReturn(List.of(hashtag));

        List<HashtagResponse> result = searchService.searchHashtags("#Spring", 10);

        assertEquals(1, result.size());
        assertEquals("springboot", result.get(0).getTag());
    }

    @Test
    void searchHashtags_ShouldThrowException_WhenQueryBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> searchService.searchHashtags(" ", 10));
    }

    @Test
    void getHashtagByTag_ShouldReturnHashtag() {
        when(hashtagRepository.findByTag("springboot")).thenReturn(Optional.of(hashtag));

        HashtagResponse result = searchService.getHashtagByTag("#SpringBoot");

        assertEquals("springboot", result.getTag());
        assertEquals(5, result.getPostCount());
    }

    @Test
    void getHashtagByTag_ShouldThrowException_WhenNotFound() {
        when(hashtagRepository.findByTag("missing")).thenReturn(Optional.empty());

        assertThrows(HashtagNotFoundException.class,
                () -> searchService.getHashtagByTag("missing"));
    }

    @Test
    void getPostCountByHashtag_ShouldReturnCount() {
        when(hashtagRepository.findByTag("springboot")).thenReturn(Optional.of(hashtag));
        when(postHashtagRepository.countByHashtagId(1L)).thenReturn(7L);

        long count = searchService.getPostCountByHashtag("#SpringBoot");

        assertEquals(7L, count);
    }
}