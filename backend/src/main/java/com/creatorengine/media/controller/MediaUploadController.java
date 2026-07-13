package com.creatorengine.media.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.BadRequestException;
import com.creatorengine.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Handles image uploads for automation DM steps (Send Image action).
 * Images are stored in Cloudinary and served over its CDN, since
 * Instagram's Send API fetches attachment images by URL server-side -
 * it cannot accept raw bytes or authenticated/private URLs.
 */
@RestController
@RequestMapping("/api/media")
public class MediaUploadController {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadController.class);

    private static final long MAX_FILE_SIZE_BYTES = 8L * 1024 * 1024; // 8MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final Cloudinary cloudinary;

    public MediaUploadController(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @PostMapping("/dm-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDmImage(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (cloudinary == null) {
            log.warn("Image upload attempted but Cloudinary is not configured.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ApiResponse.error("Image upload isn't set up yet. Add Cloudinary credentials first."));
        }

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

        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "creatorengine/dm-images/" + uid,
                        "resource_type", "image"
                )
        );

        String publicUrl = String.valueOf(result.get("secure_url"));

        log.info("Uploaded DM image for uid={} size={}bytes url={}",
                uid, file.getSize(), publicUrl);

        return ResponseEntity.ok(ApiResponse.ok(
                "Image uploaded.",
                Map.of("imageUrl", publicUrl)
        ));
    }
}