package com.kumbukaa.controller;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.AdminCreateRequest;
import com.kumbukaa.dto.AdminProfileResponse;
import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.MessageResponse;
import com.kumbukaa.dto.ResetPasswordResponse;
import com.kumbukaa.dto.UserAdminDto;
import com.kumbukaa.entity.User;
import com.kumbukaa.service.AdminService;
import com.kumbukaa.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminController(AdminService adminService, AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.adminService = adminService;
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.adminLogin(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        }
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserAdminDto>> getAllUsersWithLoans() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(adminService.listAllUsersWithLoans());
    }

    @PostMapping("/admin/users/{id}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetUserPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        adminService.resetUserPassword(id, request.getPassword());
        ResetPasswordResponse response = new ResetPasswordResponse(
                "New password created",
                request.getPassword());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        adminService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("user deleted successfully"));
    }

    // Note: createAdminUser endpoint remains for compatibility with tests and tooling.
    @PostMapping("/admins")
    public ResponseEntity<AuthResponse> createAdminUser(@RequestBody AdminCreateRequest request) {
        if (request == null
                || request.getFullName() == null || request.getFullName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()
                || request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        User createdUser = adminService.createAdminUser(
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getPassword());

        AuthResponse response = new AuthResponse(
                createdUser.getId(),
                createdUser.getEmail(),
                createdUser.getFullName(),
                "Admin user created successfully",
                jwtTokenProvider.createAccessToken(createdUser),
                jwtTokenProvider.createRefreshToken(createdUser));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public static class ResetPasswordRequest {
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/admin/profile")
    public ResponseEntity<AdminProfileResponse> getAdminProfile() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (!(principal instanceof User)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        User user = (User) principal;
        AdminProfileResponse dto = AdminProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/admin/change-password")
    public ResponseEntity<String> changeAdminPassword(@RequestBody ChangePasswordRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (request == null || request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body("New password is required");
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (!(principal instanceof User)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        User user = (User) principal;

        // If mustChangePassword is set, allow change without current password
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(request.getNewPassword()));
            user.setMustChangePassword(false);
            adminService.resetUserPassword(user.getId(), request.getNewPassword());
            return ResponseEntity.ok("Password updated");
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Current password is required");
        }

        // verify current password
        // reuse AuthService logic by fetching user from repository and checking
        if (!new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(request.getCurrentPassword(), user.getPasswordHash())
                && !computeLegacyMatch(user.getPasswordHash(), request.getCurrentPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Current password is incorrect");
        }

        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(request.getNewPassword()));
        adminService.resetUserPassword(user.getId(), request.getNewPassword());
        return ResponseEntity.ok("Password updated");
    }

    private boolean computeLegacyMatch(String storedHash, String rawPassword) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString().equalsIgnoreCase(storedHash);
        } catch (java.security.NoSuchAlgorithmException e) {
            return false;
        }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
