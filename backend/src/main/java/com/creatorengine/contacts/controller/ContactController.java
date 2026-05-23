package com.creatorengine.contacts.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.contacts.dto.ContactResponse;
import com.creatorengine.contacts.service.ContactService;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code GET /api/contacts} — list contacts for the current user.
 *
 * <p>Sorting (most-recently-interacted first) comes from the repository.
 * Searching and source-filtering happen on the frontend because the
 * per-user contact volume is bounded — fetch-once, filter-locally is
 * the simplest correct answer and matches what {@code Automations}
 * already does. If a single user ever crosses ~10k contacts we'll
 * push these filters server-side with proper Firestore composite
 * indexes; this comment is the marker for that switch.</p>
 *
 * <p>Auth: standard JWT. The user only ever sees their own contacts
 * because the service scopes every query to the resolved UID.</p>
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Read-only access to the user's contact list")
public class ContactController {

    private final ContactService service;

    @GetMapping
    @Operation(summary = "List all contacts for the current user (most-recent first)")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> list() {
        String uid = SecurityUtils.getCurrentUserId();
        List<ContactResponse> items = service.list(uid).stream()
                .map(ContactResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }
}
