package com.creatorengine.contacts.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.contacts.entity.Contact;
import com.creatorengine.contacts.service.ContactService;
import com.creatorengine.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MULTI-ACCOUNT: Contacts are scoped to the active Instagram account
 * via the X-IG-Account-Id request header (set automatically by api.js).
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Contact>>> list(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        List<Contact> contacts = contactService.list(uid, igAccountId);
        return ResponseEntity.ok(ApiResponse.ok(contacts));
    }
}