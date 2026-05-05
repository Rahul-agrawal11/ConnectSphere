package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.enums.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MediaRepositoryTest {

    @Autowired
    private MediaRepository mediaRepository;

    private Media createMedia(Long uploaderId, Long linkedPostId, MediaType mediaType, boolean deleted) {
        return Media.builder()
                .uploaderId(uploaderId)
                .url("http://localhost:8087/files/test.jpg")
                .storageKey("images/test-" + System.nanoTime() + ".jpg")
                .mediaType(mediaType)
                .sizeKb(10L)
                .mimeType(mediaType == MediaType.IMAGE ? "image/jpeg" : "video/mp4")
                .originalFilename("test.jpg")
                .linkedPostId(linkedPostId)
                .isDeleted(deleted)
                .build();
    }

    @Test
    @DisplayName("Should find active media by id")
    void findByIdAndIsDeletedFalse_ShouldReturnActiveMedia() {
        Media saved = mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, false));

        Optional<Media> found = mediaRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUploaderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should not return deleted media")
    void findByIdAndIsDeletedFalse_ShouldNotReturnDeletedMedia() {
        Media saved = mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, true));

        Optional<Media> found = mediaRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find media by uploader")
    void findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc_ShouldReturnMedia() {
        mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, false));
        mediaRepository.save(createMedia(10L, 101L, MediaType.VIDEO, false));
        mediaRepository.save(createMedia(10L, 102L, MediaType.IMAGE, true));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Media> result = mediaRepository.findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(
                10L, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find media by post")
    void findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc_ShouldReturnMedia() {
        mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, false));
        mediaRepository.save(createMedia(11L, 100L, MediaType.VIDEO, false));
        mediaRepository.save(createMedia(12L, 200L, MediaType.IMAGE, false));

        List<Media> result = mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(100L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should find media by media type")
    void findByMediaTypeAndIsDeletedFalseOrderByUploadedAtDesc_ShouldReturnMedia() {
        mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, false));
        mediaRepository.save(createMedia(11L, 101L, MediaType.VIDEO, false));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Media> result = mediaRepository.findByMediaTypeAndIsDeletedFalseOrderByUploadedAtDesc(
                MediaType.IMAGE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMediaType()).isEqualTo(MediaType.IMAGE);
    }

    @Test
    @DisplayName("Should find by uploader and media type")
    void findByUploaderIdAndMediaTypeAndIsDeletedFalseOrderByUploadedAtDesc_ShouldReturnMedia() {
        mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, false));
        mediaRepository.save(createMedia(10L, 101L, MediaType.VIDEO, false));
        mediaRepository.save(createMedia(11L, 102L, MediaType.IMAGE, false));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Media> result = mediaRepository.findByUploaderIdAndMediaTypeAndIsDeletedFalseOrderByUploadedAtDesc(
                10L, MediaType.IMAGE, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should find media by storage key")
    void findByStorageKey_ShouldReturnMedia() {
        Media saved = mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, false));

        Optional<Media> found = mediaRepository.findByStorageKey(saved.getStorageKey());

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Should count active media by uploader")
    void countByUploaderIdAndIsDeletedFalse_ShouldReturnCount() {
        mediaRepository.save(createMedia(10L, 100L, MediaType.IMAGE, false));
        mediaRepository.save(createMedia(10L, 101L, MediaType.VIDEO, false));
        mediaRepository.save(createMedia(10L, 102L, MediaType.IMAGE, true));

        long count = mediaRepository.countByUploaderIdAndIsDeletedFalse(10L);

        assertThat(count).isEqualTo(2);
    }
}