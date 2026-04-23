package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    // Find a non-deleted media record by ID
    Optional<Media> findByIdAndIsDeletedFalse(Long id);

    // All active media by a specific uploader
    Page<Media> findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(
            Long uploaderId, Pageable pageable);

    // All active media linked to a specific post
    List<Media> findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(
            Long linkedPostId);

    // All active media by type
    Page<Media> findByMediaTypeAndIsDeletedFalseOrderByUploadedAtDesc(
            MediaType mediaType, Pageable pageable);

    // Uploader + media type filter
    Page<Media> findByUploaderIdAndMediaTypeAndIsDeletedFalseOrderByUploadedAtDesc(
            Long uploaderId, MediaType mediaType, Pageable pageable);

    // Find by storage key (used for deletion from storage backend)
    Optional<Media> findByStorageKey(String storageKey);

    // Count media for a user
    long countByUploaderIdAndIsDeletedFalse(Long uploaderId);
}