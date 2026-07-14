package com.creatorengine.contacts.controller;

import com.creatorengine.common.ApiResponse;
import com.creatorengine.contacts.entity.Contact;
import com.creatorengine.contacts.service.ContactService;
import com.creatorengine.security.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    /**
     * Export all contacts for the active account as a UTF-8 CSV file.
     * Columns: username, email, instagram_id, source, total_triggers, last_interaction, created_at
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestHeader(value = "X-IG-Account-Id", required = false) String igAccountId
    ) {
        String uid = SecurityUtils.getCurrentUserId();
        List<Contact> contacts = contactService.list(uid, igAccountId);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

        StringBuilder csv = new StringBuilder();
        csv.append("username,email,instagram_id,source,total_triggers,last_interaction,created_at\n");

        for (Contact c : contacts) {
            csv.append(escapeCsv(c.getUsername()))
               .append(',').append(escapeCsv(c.getEmail()))
               .append(',').append(escapeCsv(c.getInstagramUserId()))
               .append(',').append(escapeCsv(c.getSource()))
               .append(',').append(c.getTotalTriggers())
               .append(',').append(c.getUpdatedAt() != null ? fmt.format(c.getUpdatedAt()) : "")
               .append(',').append(c.getCreatedAt() != null ? fmt.format(c.getCreatedAt()) : "")
               .append('\n');
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contacts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}