package com.connectsphere.media.service.impl;

import com.connectsphere.media.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem storage implementation.
 * Active when app.storage.provider=local (default for development).
 *
 * Files are stored at: {upload-dir}/{folder}/{uuid}_{originalFilename}
 * Files are served at: {base-url}/{folder}/{uuid}_{originalFilename}
 *
 * For production, swap this with S3StorageServiceImpl by setting
 * app.storage.provider=s3 in application.yml.
 */
@Slf4j
@Service
@ConditionalOnProperty(
        name = "app.storage.provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalStorageServiceImpl implements StorageService {

    @Value("${app.storage.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.storage.local.base-url:http://localhost:8087/files}")
    private String baseUrl;

    /**
     * Create the upload directory on startup if it doesn't exist.
     */
    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public StorageResult store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty");
        }

        try {
            // Build target directory
            Path targetDir = Paths.get(uploadDir, folder);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Generate unique filename to prevent collisions
            String originalFilename = sanitizeFilename(
                    file.getOriginalFilename());
            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

            // Copy file to target location
            Path targetPath = targetDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath,
                    StandardCopyOption.REPLACE_EXISTING);

            // Build public URL and storage key
            String storageKey = folder + "/" + uniqueFilename;
            String publicUrl = baseUrl + "/" + storageKey;

            log.info("File stored locally: key={} url={}", storageKey, publicUrl);

            return new StorageResult(publicUrl, storageKey);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path filePath = Paths.get(uploadDir, storageKey);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted from local storage: {}", storageKey);
            } else {
                log.warn("File not found for deletion: {}", storageKey);
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", storageKey, e.getMessage());
        }
    }

    /**
     * Sanitize filename — remove path traversal characters
     * and replace spaces with underscores.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .toLowerCase();
    }
}