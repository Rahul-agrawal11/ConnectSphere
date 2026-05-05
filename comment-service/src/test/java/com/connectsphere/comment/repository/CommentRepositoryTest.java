package com.connectsphere.comment.repository;

import com.connectsphere.comment.entity.Comment;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    private Comment createComment(Long postId, Long authorId, Long parentCommentId, String content) {
        return Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .parentCommentId(parentCommentId)
                .content(content)
                .likesCount(0)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Should find active comment by id")
    void findByIdAndIsDeletedFalse_ShouldReturnComment() {
        Comment saved = commentRepository.save(createComment(100L, 10L, null, "Nice post"));

        Optional<Comment> found = commentRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Nice post");
    }

    @Test
    @DisplayName("Should not return deleted comment")
    void findByIdAndIsDeletedFalse_ShouldNotReturnDeletedComment() {
        Comment comment = createComment(100L, 10L, null, "Deleted comment");
        comment.setIsDeleted(true);
        Comment saved = commentRepository.save(comment);

        Optional<Comment> found = commentRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find top-level comments by post id")
    void findTopLevelByPostId_ShouldReturnOnlyTopLevelComments() {
        Comment parent = commentRepository.save(createComment(100L, 10L, null, "Parent comment"));
        commentRepository.save(createComment(100L, 20L, parent.getId(), "Reply comment"));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Comment> result = commentRepository.findTopLevelByPostId(100L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getParentCommentId()).isNull();
    }

    @Test
    @DisplayName("Should find replies by parent comment id")
    void findRepliesByParentCommentId_ShouldReturnReplies() {
        Comment parent = commentRepository.save(createComment(100L, 10L, null, "Parent comment"));
        commentRepository.save(createComment(100L, 20L, parent.getId(), "Reply one"));
        commentRepository.save(createComment(100L, 30L, parent.getId(), "Reply two"));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Comment> result = commentRepository.findRepliesByParentCommentId(parent.getId(), pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find comments by author")
    void findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc_ShouldReturnComments() {
        commentRepository.save(createComment(100L, 10L, null, "Comment one"));
        commentRepository.save(createComment(101L, 10L, null, "Comment two"));
        commentRepository.save(createComment(102L, 20L, null, "Other user comment"));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Comment> result =
                commentRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(10L, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should count top-level comments only")
    void countTopLevelByPostId_ShouldReturnOnlyParentCount() {
        Comment parent = commentRepository.save(createComment(100L, 10L, null, "Parent comment"));
        commentRepository.save(createComment(100L, 20L, parent.getId(), "Reply comment"));
        commentRepository.save(createComment(100L, 30L, null, "Another parent"));

        long count = commentRepository.countTopLevelByPostId(100L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count all comments including replies")
    void countAllByPostId_ShouldReturnTotalCount() {
        Comment parent = commentRepository.save(createComment(100L, 10L, null, "Parent comment"));
        commentRepository.save(createComment(100L, 20L, parent.getId(), "Reply comment"));
        commentRepository.save(createComment(100L, 30L, null, "Another parent"));

        long count = commentRepository.countAllByPostId(100L);

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should increment likes count")
    void incrementLikesCount_ShouldIncreaseLikes() {
        Comment saved = commentRepository.saveAndFlush(
                createComment(100L, 10L, null, "Like comment"));

        int updated = commentRepository.incrementLikesCount(saved.getId());

        entityManager.clear();

        assertThat(updated).isEqualTo(1);

        Comment found = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should decrement likes count")
    void decrementLikesCount_ShouldDecreaseLikes() {
        Comment comment = createComment(100L, 10L, null, "Unlike comment");
        comment.setLikesCount(2);

        Comment saved = commentRepository.saveAndFlush(comment);

        int updated = commentRepository.decrementLikesCount(saved.getId());

        entityManager.clear();

        assertThat(updated).isEqualTo(1);

        Comment found = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikesCount()).isEqualTo(1);
    }
}