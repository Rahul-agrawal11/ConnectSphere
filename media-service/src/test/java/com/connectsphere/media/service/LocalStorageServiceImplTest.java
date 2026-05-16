package com.connectsphere.media.service;

import com.connectsphere.media.exception.StorageException;
import com.connectsphere.media.service.impl.LocalStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class LocalStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private LocalStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "baseUrl", "http://localhost:8087/files");
        storageService.init();
    }

    @Test
    void store_validImageFile_shouldReturnUrlAndKey() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "image-bytes".getBytes());

        StorageService.StorageResult result = storageService.store(file, "images");

        assertThat(result.url()).startsWith("http://localhost:8087/files/images/");
        assertThat(result.storageKey()).startsWith("images/");
        assertThat(result.storageKey()).endsWith("photo.jpg");
    }

    @Test
    void store_createsSubfolderIfAbsent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "story.jpg", "image/jpeg", "bytes".getBytes());

        assertThatNoException().isThrownBy(() -> storageService.store(file, "stories"));
    }

    @Test
    void store_nullFile_shouldThrow() {
        assertThatThrownBy(() -> storageService.store(null, "images"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File must not be null or empty");
    }

    @Test
    void store_emptyFile_shouldThrow() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.store(empty, "images"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_sanitizesFilenameWithSpecialChars() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my file (1).jpg", "image/jpeg", "data".getBytes());

        StorageService.StorageResult result = storageService.store(file, "images");

        // Spaces and parens should be replaced with underscores
        assertThat(result.storageKey()).doesNotContain(" ", "(", ")");
    }

    @Test
    void store_nullOriginalFilename_shouldUseDefaultName() {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "image/jpeg", "data".getBytes());

        StorageService.StorageResult result = storageService.store(file, "images");

        assertThat(result.storageKey()).contains("file");
    }

    @Test
    void delete_existingFile_shouldDeleteSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "todelete.jpg", "image/jpeg", "data".getBytes());
        StorageService.StorageResult result = storageService.store(file, "images");

        assertThatNoException().isThrownBy(() -> storageService.delete(result.storageKey()));
    }

    @Test
    void delete_nonExistentFile_shouldNotThrow() {
        assertThatNoException().isThrownBy(() -> storageService.delete("images/nonexistent.jpg"));
    }

    @Test
    void init_invalidUploadDir_shouldThrowStorageException(@TempDir Path tempDir) throws Exception {
        // Create a FILE at the path — so treating it as a directory will fail
        Path blockingFile = tempDir.resolve("not-a-dir");
        Files.createFile(blockingFile);
        // Point uploadDir to a subdirectory of that file (impossible to create)
        String impossibleDir = blockingFile.toString() + "/sub";

        LocalStorageServiceImpl failingService = new LocalStorageServiceImpl();
        ReflectionTestUtils.setField(failingService, "uploadDir", impossibleDir);
        ReflectionTestUtils.setField(failingService, "baseUrl", "http://localhost:8087/files");

        assertThatThrownBy(failingService::init)
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Could not create upload directory");
    }
}