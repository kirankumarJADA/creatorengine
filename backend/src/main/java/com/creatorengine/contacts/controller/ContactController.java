package com.creatorengine.contacts.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.contacts.dto.ContactResponse;
import com.creatorengine.contacts.service.ContactService;
import com.creatorengine.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contacts", description = "Read-only access to the user's contact list")
public class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

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