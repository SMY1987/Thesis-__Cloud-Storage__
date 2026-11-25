package ru.example.cloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import ru.example.cloud.dto.FileResponse;
import ru.example.cloud.entity.StorageFile;
import ru.example.cloud.entity.User;
import ru.example.cloud.exception.AppException;
import ru.example.cloud.repository.StorageFileRepository;
import ru.example.cloud.repository.UserRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private StorageFileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    private FileStorageService fileStorageService;

    private User testUser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(fileRepository, userRepository, tempDir);
        testUser = new User();
    }

    @Test
    void uploadSavesFileToStorageAndMetadataToDb() throws Exception {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello world".getBytes());

        fileStorageService.upload("user", "test.txt", file);

        Path stored = tempDir.resolve("1_test.txt");
        assertTrue(java.nio.file.Files.exists(stored));
        assertEquals("hello world", java.nio.file.Files.readString(stored));

        ArgumentCaptor<StorageFile> captor = ArgumentCaptor.forClass(StorageFile.class);
        verify(fileRepository).save(captor.capture());
        StorageFile saved = captor.getValue();
        assertEquals("test.txt", saved.getFilename());
        assertEquals(11L, saved.getSize());
    }

    @Test
    void uploadEmptyFileThrows400() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        AppException ex = assertThrows(AppException.class,
                () -> fileStorageService.upload("user", "empty.txt", emptyFile));

        assertEquals(400, ex.getStatus());
    }

    @Test
    void deleteRemovesFileFromDiskAndDb() throws Exception {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        Path stored = tempDir.resolve("1_test.txt");
        java.nio.file.Files.writeString(stored, "data");
        StorageFile metadata = new StorageFile(
                null, "test.txt", testUser, 4L, stored.toString(), null);
        when(fileRepository.findByFilenameAndUser("test.txt", testUser))
                .thenReturn(Optional.of(metadata));

        fileStorageService.delete("user", "test.txt");

        assertTrue(!java.nio.file.Files.exists(stored));
        verify(fileRepository).delete(metadata);
    }

    @Test
    void deleteUnknownFileThrows400() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByFilenameAndUser("no-such-file.txt", testUser))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> fileStorageService.delete("user", "no-such-file.txt"));

        assertEquals(400, ex.getStatus());
    }

    @Test
    void downloadReturnsReadableResource() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        Path stored = tempDir.resolve("1_test.txt");
        try {
            java.nio.file.Files.writeString(stored, "content");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        StorageFile metadata = new StorageFile(
                null, "test.txt", testUser, 7L, stored.toString(), null);
        when(fileRepository.findByFilenameAndUser("test.txt", testUser))
                .thenReturn(Optional.of(metadata));

        var resource = fileStorageService.download("user", "test.txt");

        assertTrue(resource.exists());
    }

    @Test
    void renameChangesFilename() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        Path stored = tempDir.resolve("1_test.txt");
        StorageFile metadata = new StorageFile(
                null, "test.txt", testUser, 7L, stored.toString(), null);
        when(fileRepository.findByFilenameAndUser("test.txt", testUser))
                .thenReturn(Optional.of(metadata));
        when(fileRepository.findByFilenameAndUser("renamed.txt", testUser))
                .thenReturn(Optional.empty());

        fileStorageService.rename("user", "test.txt", "renamed.txt");

        ArgumentCaptor<StorageFile> captor = ArgumentCaptor.forClass(StorageFile.class);
        verify(fileRepository).save(captor.capture());
        assertEquals("renamed.txt", captor.getValue().getFilename());
    }

    @Test
    void renameToExistingNameThrows400() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        Path stored = tempDir.resolve("1_test.txt");
        StorageFile existing = new StorageFile(
                null, "test.txt", testUser, 7L, stored.toString(), null);
        StorageFile conflict = new StorageFile(
                null, "taken.txt", testUser, 7L, stored.toString(), null);

        when(fileRepository.findByFilenameAndUser("test.txt", testUser))
                .thenReturn(Optional.of(existing));
        when(fileRepository.findByFilenameAndUser("taken.txt", testUser))
                .thenReturn(Optional.of(conflict));

        AppException ex = assertThrows(AppException.class,
                () -> fileStorageService.rename("user", "test.txt", "taken.txt"));

        assertEquals(400, ex.getStatus());
    }

    @Test
    void listReturnsFilesWithLimit() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        StorageFile f1 = new StorageFile(null, "a.txt", testUser, 10L, "/tmp/a.txt", null);
        StorageFile f2 = new StorageFile(null, "b.txt", testUser, 20L, "/tmp/b.txt", null);
        Page<StorageFile> page = new PageImpl<>(List.of(f1, f2));
        when(fileRepository.findByUser(any(User.class), any(PageRequest.class)))
                .thenReturn(page);

        List<FileResponse> result = fileStorageService.list("user", 10);

        assertEquals(2, result.size());
        assertEquals("a.txt", result.get(0).getClass());
        assertEquals(10L, result.get(0).getSize());
        assertEquals("b.txt", result.get(1).getClass());
        assertEquals(20L, result.get(1).getSize());
    }
}