package ru.example.cloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    @Bean
    public Path storagePath(@Value("${app.storage-path}") String path) throws IOException {
        Path dir = Paths.get(path);
        Files.createDirectories(dir);
        return dir;
    }
}