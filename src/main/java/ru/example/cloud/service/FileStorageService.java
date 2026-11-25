package ru.example.cloud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.example.cloud.dto.FileResponse;
import ru.example.cloud.entity.StorageFile;
import ru.example.cloud.entity.User;
import ru.example.cloud.exception.AppException;
import ru.example.cloud.repository.StorageFileRepository;
import ru.example.cloud.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StorageFileRepository fileRepository;
    private final UserRepository userRepository;
    private final Path storagePath;

    @Transactional
    public void upload(String login, String filename, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AppException(400, "Error input data");
        }
        if (filename == null || filename.isBlank()) {
            throw new AppException(400, "Error input data");
        }

        User user = getUser(login);
        String safeName = sanitize(filename);

        Path target = storagePath.resolve(user.getId() + "_" + safeName);
        file.transferTo(target);

        fileRepository.save(new StorageFile(
                null, safeName, user, file.getSize(), target.toString(), LocalDateTime.now()));
    }

    @Transactional
    public void delete(String login, String filename) throws IOException {
        StorageFile storageFile = findFile(login, filename);

        Files.deleteIfExists(Paths.get(storageFile.getStoragePath()));
        fileRepository.delete(storageFile);
    }

    @Transactional(readOnly = true)
    public Resource download(String login, String filename) {
        StorageFile storageFile = findFile(login, filename);
        return new FileSystemResource(storageFile.getStoragePath());
    }

    @Transactional
    public void rename(String login, String oldFilename, String newFilename) {
        if (newFilename == null || newFilename.isBlank()) {
            throw new AppException(400, "Error input data");
        }

        StorageFile storageFile = findFile(login, oldFilename);
        String safeName = sanitize(newFilename);

        fileRepository.findByFilenameAndUser(safeName, storageFile.getUser())
                .ifPresent(f -> {
                    throw new AppException(400, "Error input data");
                });

        storageFile.setFilename(safeName);
        fileRepository.save(storageFile);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> list(String login, int limit) {
        User user = getUser(login);
        int size = limit > 0 ? limit : 100;

        return fileRepository.findByUser(user, PageRequest.of(0, size)).getContent().stream()
                .map(f -> new FileResponse(f.getFilename(), f.getSize()))
                .toList();
    }

    private StorageFile findFile(String login, String filename) {
        User user = getUser(login);
        return fileRepository.findByFilenameAndUser(filename, user)
                .orElseThrow(() -> new AppException(400, "Error input data"));
    }

    private User getUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new AppException(401, "Unauthorized error"));
    }

    private String sanitize(String filename) {
        return Paths.get(filename).getFileName().toString();
    }
}