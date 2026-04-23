package com.connectsphere.media.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage service contract.
 *
 * Abstracts the file storage backend behind an interface.
 * Two implementations are provided:
 *   LocalStorageServiceImpl — stores files on the local filesystem (dev)
 *   S3StorageServiceImpl    — stores files on AWS S3 (production)
 *
 * Switch between them by setting app.storage.provider in application.yml.
 *
 * This pattern means the rest of the codebase is storage-agnostic
 * and S3 migration requires zero business logic changes.
 */
public interface StorageService {

    /**
     * Store a file and return its public-accessible URL.
     *
     * @param file      the uploaded multipart file
     * @param folder    subfolder path (e.g. "posts", "stories", "avatars")
     * @return          StorageResult containing the public URL and storage key
     */
    StorageResult store(MultipartFile file, String folder);

    /**
     * Delete a file by its storage key.
     *
     * @param storageKey the key returned by store() (local path or S3 key)
     */
    void delete(String storageKey);

    /**
     * Result object returned by store().
     *
     * @param url        public URL to access the file
     * @param storageKey internal key used for deletion (S3 key or local path)
     */
    record StorageResult(String url, String storageKey) {}
}