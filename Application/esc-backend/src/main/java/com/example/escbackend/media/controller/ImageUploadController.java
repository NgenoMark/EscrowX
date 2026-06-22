package com.example.escbackend.media.controller;

import com.example.escbackend.media.dto.UploadImageResponse;
import com.example.escbackend.media.service.ImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ImageUploadController {

    private final ImageStorageService imageStorageService;

    public ImageUploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(value = "/disputes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadImageResponse uploadDisputeImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String referenceId
    ) {
        String relativeUrl = imageStorageService.storeDisputeImage(file, referenceId);
        return UploadImageResponse.builder().url(relativeUrl).build();
    }

    @PostMapping(value = "/users/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadImageResponse uploadProfileImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String userId
    ) {
        String relativeUrl = imageStorageService.storeUserProfileImage(file, userId);
        return UploadImageResponse.builder().url(relativeUrl).build();
    }
}
