package ru.example.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.example.cloud.dto.FileResponse;
import ru.example.cloud.dto.RenameRequest;
import ru.example.cloud.service.AuthService;
import ru.example.cloud.service.FileStorageService;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final AuthService authService;

    @PostMapping("/file")
    public void upload(@RequestParam("file") MultipartFile file,
                       @RequestParam("filename") String filename,
                       @RequestHeader("auth-token") String token) throws IOException {
        fileStorageService.upload(authService.getLoginByToken(token), filename, file);
    }

    @DeleteMapping("/file")
    public void delete(@RequestParam("filename") String filename,
                       @RequestHeader("auth-token") String token) throws IOException {
        fileStorageService.delete(authService.getLoginByToken(token), filename);
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> download(@RequestParam("filename") String filename,
                                             @RequestHeader("auth-token") String token) {
        Resource resource = fileStorageService.download(authService.getLoginByToken(token), filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PutMapping("/file")
    public void rename(@RequestParam("filename") String filename,
                       @RequestBody RenameRequest body,
                       @RequestHeader("auth-token") String token) {
        fileStorageService.rename(authService.getLoginByToken(token), filename, body.getName());
    }

    @GetMapping("/list")
    public List<FileResponse> list(@RequestParam(value = "limit", defaultValue = "100") int limit,
                                   @RequestHeader("auth-token") String token) {
        return fileStorageService.list(authService.getLoginByToken(token), limit);
    }
}