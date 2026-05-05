package com.connectsphere.post.repository;

import com.connectsphere.post.entity.Post;
import com.connectsphere.post.enums.PostVisibility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Post createPost(Long authorId, String content, PostVisibility visibility) {
        return Post.builder()
                .authorId(authorId)
                .content(content)
                .visibility(visibility)
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Should find active post by id")
    void findByIdAndIsDeletedFalse_ShouldReturnPost() {
        Post saved = postRepository.save(createPost(10L, "Hello Post", PostVisibility.PUBLIC));

        Optional<Post> found = postRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Hello Post");
    }

    @Test
    @DisplayName("Should not return soft deleted post")
    void findByIdAndIsDeletedFalse_ShouldNotReturnDeletedPost() {
        Post post = createPost(10L, "Deleted Post", PostVisibility.PUBLIC);
        post.setIsDeleted(true);
        Post saved = postRepository.save(post);

        Optional<Post> found = postRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all active posts by author")
    void findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc_ShouldReturnPosts() {
        postRepository.save(createPost(10L, "Post 1", PostVisibility.PUBLIC));
        postRepository.save(createPost(10L, "Post 2", PostVisibility.PRIVATE));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(10L, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find public posts by author")
    void findByAuthorIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtDesc_ShouldReturnPublicPosts() {
        postRepository.save(createPost(10L, "Public Post", PostVisibility.PUBLIC));
        postRepository.save(createPost(10L, "Private Post", PostVisibility.PRIVATE));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.findByAuthorIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                10L, PostVisibility.PUBLIC, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getVisibility()).isEqualTo(PostVisibility.PUBLIC);
    }

    @Test
    @DisplayName("Should find public feed")
    void findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc_ShouldReturnPublicFeed() {
        postRepository.save(createPost(10L, "Public Post", PostVisibility.PUBLIC));
        postRepository.save(createPost(11L, "Private Post", PostVisibility.PRIVATE));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
                PostVisibility.PUBLIC, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Public Post");
    }

    @Test
    @DisplayName("Should search public posts by content")
    void searchByContent_ShouldReturnMatchingPublicPosts() {
        postRepository.save(createPost(10L, "Java Spring Boot Post", PostVisibility.PUBLIC));
        postRepository.save(createPost(11L, "React Frontend Post", PostVisibility.PUBLIC));
        postRepository.save(createPost(12L, "Private Java Post", PostVisibility.PRIVATE));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.searchByContent("java", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).contains("Java");
    }

    @Test
    @DisplayName("Should find feed by followed author ids")
    void findFeedByAuthorIds_ShouldReturnPublicAndFollowersOnlyPosts() {
        postRepository.save(createPost(10L, "Public Post", PostVisibility.PUBLIC));
        postRepository.save(createPost(11L, "Followers Post", PostVisibility.FOLLOWERS_ONLY));
        postRepository.save(createPost(12L, "Private Post", PostVisibility.PRIVATE));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.findFeedByAuthorIds(List.of(10L, 11L, 12L), pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should count active posts by author")
    void countByAuthorIdAndIsDeletedFalse_ShouldReturnCount() {
        postRepository.save(createPost(10L, "Post 1", PostVisibility.PUBLIC));
        postRepository.save(createPost(10L, "Post 2", PostVisibility.PUBLIC));

        long count = postRepository.countByAuthorIdAndIsDeletedFalse(10L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should increment likes count")
    void incrementLikesCount_ShouldIncreaseLikes() {
        Post saved = postRepository.saveAndFlush(createPost(10L, "Like Post", PostVisibility.PUBLIC));

        int updated = postRepository.incrementLikesCount(saved.getId());

        entityManager.clear();

        assertThat(updated).isEqualTo(1);

        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should increment comments count")
    void incrementCommentsCount_ShouldIncreaseComments() {
        Post saved = postRepository.saveAndFlush(createPost(10L, "Comment Post", PostVisibility.PUBLIC));

        int updated = postRepository.incrementCommentsCount(saved.getId());

        entityManager.clear();

        assertThat(updated).isEqualTo(1);

        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCommentsCount()).isEqualTo(1);
    }
}