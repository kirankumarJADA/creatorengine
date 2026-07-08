package com.creatorengine.admin.controller;

import com.creatorengine.admin.dto.AdminUserResponse;
import com.creatorengine.auth.entity.User;
import com.creatorengine.auth.repository.UserRepository;
import com.creatorengine.common.ApiResponse;
import com.creatorengine.exception.ResourceNotFoundException;
import com.creatorengine.instagram.entity.InstagramAccount;
import com.creatorengine.instagram.repository.InstagramAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final InstagramAccountRepository instagramAccountRepository;

    public AdminUserController(
            UserRepository userRepository,
            InstagramAccountRepository instagramAccountRepository
    ) {
        this.userRepository = userRepository;
        this.instagramAccountRepository = instagramAccountRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> list() {
        List<AdminUserResponse> result = userRepository.findAll().stream()
                .map(u -> {
                    Optional<InstagramAccount> ig = instagramAccountRepository.findByUid(u.getUid());
                    boolean connected = ig.map(InstagramAccount::getConnected).orElse(false);
                    String username = ig.map(InstagramAccount::getUsername).orElse(null);
                    return AdminUserResponse.from(u, connected, username);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/{uid}/enable")
    public ResponseEntity<ApiResponse<AdminUserResponse>> enable(@PathVariable String uid) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));
        user.setEnabled(true);
        userRepository.save(user);

        Optional<InstagramAccount> ig = instagramAccountRepository.findByUid(uid);
        return ResponseEntity.ok(ApiResponse.ok("User enabled.",
                AdminUserResponse.from(user,
                        ig.map(InstagramAccount::getConnected).orElse(false),
                        ig.map(InstagramAccount::getUsername).orElse(null))));
    }

    @PatchMapping("/{uid}/disable")
    public ResponseEntity<ApiResponse<AdminUserResponse>> disable(@PathVariable String uid) {
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));
        user.setEnabled(false);
        userRepository.save(user);

        Optional<InstagramAccount> ig = instagramAccountRepository.findByUid(uid);
        return ResponseEntity.ok(ApiResponse.ok("User disabled.",
                AdminUserResponse.from(user,
                        ig.map(InstagramAccount::getConnected).orElse(false),
                        ig.map(InstagramAccount::getUsername).orElse(null))));
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String uid) {
        userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", uid));

        userRepository.deleteById(uid);
        return ResponseEntity.ok(ApiResponse.<Void>ok("User deleted.", null));
    }
}