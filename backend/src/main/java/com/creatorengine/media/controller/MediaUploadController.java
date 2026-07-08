package com.creatorengine.media.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.security.SecurityUtils;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles image uploads for automation DM steps (Send Image action).
 * Images are stored in Firebase Storage and made public, since Instagram's
 * Send API fetches attachment images by URL server-side - it cannot accept
 * raw bytes or authenticated/private URLs.
 */
@RestController
@RequestMapping("/api/media")
public class MediaUploadController {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadController.class);

    private static final long MAX_FILE_SIZE_BYTES = 8L * 1024 * 1024; // 8MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final Bucket bucket;

    public MediaUploadController(Bucket bucket) {
        this.bucket = bucket;
    }

    @PostMapping("/dm-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDmImage(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String uid = SecurityUtils.getCurrentUserId();

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Image must be smaller than 8MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG, WEBP, or GIF images are allowed.");
        }

        String extension = extensionFor(contentType);
        String objectPath = "dm-images/%s/%s%s".formatted(uid, UUID.randomUUID(), extension);

        Blob blob = bucket.create(objectPath, file.getBytes(), contentType);
        blob.createAcl(com.google.cloud.storage.Acl.of(
                com.google.cloud.storage.Acl.User.ofAllUsers(),
                com.google.cloud.storage.Acl.Role.READER
        ));

        String publicUrl = "https://storage.googleapis.com/%s/%s"
                .formatted(bucket.getName(), objectPath);

        log.info("Uploaded DM image for uid={} path={} size={}bytes",
                uid, objectPath, file.getSize());

        return ResponseEntity.ok(ApiResponse.ok(
                "Image uploaded.",
                Map.of("imageUrl", publicUrl)
        ));
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }
}