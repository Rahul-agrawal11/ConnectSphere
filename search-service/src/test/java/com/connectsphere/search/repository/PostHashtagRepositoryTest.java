package com.connectsphere.search.repository;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostHashtag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostHashtagRepositoryTest {

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private PostHashtagRepository postHashtagRepository;

    private Hashtag createHashtag(String tag) {
        return hashtagRepository.save(
                Hashtag.builder()
                        .tag(tag)
                        .postCount(1)
                        .build()
        );
    }

    private PostHashtag createPostHashtag(Long postId, Long hashtagId) {
        return PostHashtag.builder()
                .postId(postId)
                .hashtagId(hashtagId)
                .build();
    }

    @Test
    @DisplayName("Should find hashtag ids by post id")
    void findHashtagIdsByPostId_ShouldReturnIds() {
        Hashtag java = createHashtag("java");
        Hashtag spring = createHashtag("springboot");

        postHashtagRepository.save(createPostHashtag(100L, java.getId()));
        postHashtagRepository.save(createPostHashtag(100L, spring.getId()));

        List<Long> result = postHashtagRepository.findHashtagIdsByPostId(100L);

        assertThat(result).containsExactlyInAnyOrder(java.getId(), spring.getId());
    }

    @Test
    @DisplayName("Should find post ids by hashtag id")
    void findPostIdsByHashtagId_ShouldReturnPostIds() {
        Hashtag java = createHashtag("java");

        postHashtagRepository.save(createPostHashtag(100L, java.getId()));
        postHashtagRepository.save(createPostHashtag(101L, java.getId()));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Long> result =
                postHashtagRepository.findPostIdsByHashtagId(java.getId(), pageable);

        assertThat(result.getContent()).containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    @DisplayName("Should check mapping exists")
    void existsByPostIdAndHashtagId_ShouldReturnTrue() {
        Hashtag java = createHashtag("java");

        postHashtagRepository.save(createPostHashtag(100L, java.getId()));

        boolean exists =
                postHashtagRepository.existsByPostIdAndHashtagId(100L, java.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should delete mappings by post id")
    void deleteByPostId_ShouldDeleteMappings() {
        Hashtag java = createHashtag("java");
        Hashtag spring = createHashtag("springboot");

        postHashtagRepository.save(createPostHashtag(100L, java.getId()));
        postHashtagRepository.save(createPostHashtag(100L, spring.getId()));
        postHashtagRepository.flush();

        postHashtagRepository.deleteByPostId(100L);

        List<Long> result = postHashtagRepository.findHashtagIdsByPostId(100L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should count posts by hashtag id")
    void countByHashtagId_ShouldReturnCount() {
        Hashtag java = createHashtag("java");

        postHashtagRepository.save(createPostHashtag(100L, java.getId()));
        postHashtagRepository.save(createPostHashtag(101L, java.getId()));

        long count = postHashtagRepository.countByHashtagId(java.getId());

        assertThat(count).isEqualTo(2);
    }
}