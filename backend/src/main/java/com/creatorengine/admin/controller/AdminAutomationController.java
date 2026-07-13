package com.creatorengine.admin.controller;

import com.creatorengine.admin.dto.AdminAutomationResponse;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.automation.entity.Automation;
import com.creatorengine.automation.repository.AutomationRepository;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/automations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAutomationController {

    private final AutomationRepository automationRepository;
    private final UserRepository userRepository;

    public AdminAutomationController(
            AutomationRepository automationRepository,
            UserRepository userRepository
    ) {
        this.automationRepository = automationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminAutomationResponse>>> list() {
        Map<String, String> emailByUid = new HashMap<>();
        userRepository.findAll().forEach(u -> emailByUid.put(u.getUid(), u.getEmail()));

        List<AdminAutomationResponse> result = automationRepository.findAllAcrossUsers().stream()
                .map(o -> AdminAutomationResponse.from(o.uid(), emailByUid.get(o.uid()), o.automation()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/{uid}/{id}/toggle")
    public ResponseEntity<ApiResponse<AdminAutomationResponse>> toggle(
            @PathVariable String uid, @PathVariable String id
    ) {
        Automation a = automationRepository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Automation", id));

        a.setEnabled(!a.getEnabled());
        Automation saved = automationRepository.save(uid, a);

        String email = userRepository.findById(uid).map(u -> u.getEmail()).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("Automation toggled.",
                AdminAutomationResponse.from(uid, email, saved)));
    }

    @DeleteMapping("/{uid}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String uid, @PathVariable String id
    ) {
        automationRepository.findById(uid, id)
                .orElseThrow(() -> new ResourceNotFoundException("Automation", id));

        automationRepository.deleteById(uid, id);
        return ResponseEntity.ok(ApiResponse.<Void>ok("Automation deleted.", null));
    }
}