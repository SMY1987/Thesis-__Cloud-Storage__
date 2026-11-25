package ru.example.cloud.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.example.cloud.entity.StorageFile;
import ru.example.cloud.entity.User;

import java.util.Optional;

public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {
    Optional<StorageFile> findByFilenameAndUser(String filename, User user);

    Page<StorageFile> findByUser(User user, Pageable pageable);
}