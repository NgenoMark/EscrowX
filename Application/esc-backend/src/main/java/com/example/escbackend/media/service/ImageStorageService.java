package com.example.escbackend.media.service;

import com.example.escbackend.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final Path baseUploadPath;

    public ImageStorageService(@Value("${escrowx.upload.base-dir:src/main/resources/static/uploads}") String baseDir) {
        this.baseUploadPath = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseUploadPath.resolve("disputes"));
            Files.createDirectories(this.baseUploadPath.resolve("users"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize upload directories", e);
        }
    }

    public String storeDisputeImage(MultipartFile file, String referenceId) {
        String referenceSegment = sanitizeSegment(referenceId == null || referenceId.isBlank() ? "general" : referenceId);
        Path targetDir = baseUploadPath.resolve("disputes").resolve(referenceSegment);
        return storeFile(file, targetDir, "/uploads/disputes/" + referenceSegment + "/");
    }

    public String storeUserProfileImage(MultipartFile file, String userId) {
        String userSegment = sanitizeSegment(userId == null || userId.isBlank() ? "anonymous" : userId);
        Path targetDir = baseUploadPath.resolve("users").resolve(userSegment);
        return storeFile(file, targetDir, "/uploads/users/" + userSegment + "/");
    }

    private String storeFile(MultipartFile file, Path targetDir, String urlPrefix) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is required.");
        }

        try {
            Files.createDirectories(targetDir);
            String extension = getFileExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path destination = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return urlPrefix + filename;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image file.");
        }
    }

    private String sanitizeSegment(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String originalName) {
        if (originalName == null || originalName.isBlank() || !originalName.contains(".")) {
            return ".jpg";
        }
        String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        if (ext.length() > 8) {
            return ".jpg";
        }
        return ext;
    }
}
