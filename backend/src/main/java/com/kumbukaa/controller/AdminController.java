package com.kumbukaa.controller;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.AdminCreateRequest;
import com.kumbukaa.dto.AdminProfileResponse;
import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.MessageResponse;
import com.kumbukaa.dto.ResetPasswordResponse;
import com.kumbukaa.dto.UserDetailDto;
import com.kumbukaa.dto.UserSummaryDto;
import com.kumbukaa.entity.User;
import com.kumbukaa.service.AdminService;
import com.kumbukaa.service.AuthService;
import com.kumbukaa.util.UserRoleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final String defaultAdminEmail;

    @Autowired
    public AdminController(
            AdminService adminService,
            AuthService authService,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.admin.email:admin@localhost}") String defaultAdminEmail) {
        this.adminService = adminService;
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.defaultAdminEmail = defaultAdminEmail;
    }

    public AdminController(AdminService adminService, AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this(adminService, authService, jwtTokenProvider, "admin@localhost");
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.ok(authService.adminLogin(request, httpRequest.getHeader("User-Agent")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        }
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserSummaryDto>> getAllUsersWithLoans() {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(adminService.listAllUsersWithLoans());
    }

    @GetMapping("/admin/users/{id}")
    public ResponseEntity<UserDetailDto> getUserById(@PathVariable Long id) {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/admin/users/{id}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetUserPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            adminService.resetUserPassword(id, request.getPassword());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        ResetPasswordResponse response = new ResetPasswordResponse(
                "New password created",
                request.getPassword());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User admin)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (admin.getId().equals(id)) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        try {
            adminService.deleteUser(id, admin);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new MessageResponse("user deleted successfully"));
    }

    @PostMapping("/admins")
    public ResponseEntity<AuthResponse> createAdminUser(@RequestBody AdminCreateRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

        User createdUser;
        try {
            createdUser = adminService.createAdminUser(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPhoneNumber(),
                    request.getPassword());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        }

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

    private boolean isDesignatedAdmin(String email) {
        if (email == null || email.isBlank()) return false;
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("admin@localhost")
                || (defaultAdminEmail != null && normalized.equals(defaultAdminEmail.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (UserRoleUtils.hasAuthority(auth, "ADMIN")) return true;
        if (auth != null && auth.getPrincipal() instanceof User user) {
            if (UserRoleUtils.hasRole(user.getRoles(), "ADMIN")) return true;
            if (isDesignatedAdmin(user.getEmail())) return true;
        }
        return false;
    }

    private boolean isSupport() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (UserRoleUtils.hasAuthority(auth, "SUPPORT_AGENT")) return true;
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return UserRoleUtils.hasRole(user.getRoles(), "SUPPORT_AGENT");
        }
        return false;
    }

    private boolean canReadAdminData() {
        return isAdmin() || isSupport();
    }

    @GetMapping("/admin/profile")
    public ResponseEntity<AdminProfileResponse> getAdminProfile() {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (!(principal instanceof User)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        User user = (User) principal;
        AdminProfileResponse dto = AdminProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .accessRole(isAdmin() ? "ADMIN" : "SUPPORT")
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

        boolean mustChangePassword = Boolean.TRUE.equals(user.getMustChangePassword());
        if (!mustChangePassword && (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank())) {
            return ResponseEntity.badRequest().body("Current password is required");
        }

        if (!mustChangePassword
                && !new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(request.getCurrentPassword(), user.getPasswordHash())
                && !computeLegacyMatch(user.getPasswordHash(), request.getCurrentPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Current password is incorrect");
        }

        adminService.changeAdminPassword(user.getId(), request.getNewPassword());
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

    @GetMapping("/admin/overview")
    public ResponseEntity<?> overview() {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(adminService.overview());
    }

    @GetMapping("/admin/loans")
    public ResponseEntity<?> globalLoans() {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(adminService.globalLoans());
    }

    @GetMapping("/admin/audit-log")
    public ResponseEntity<?> auditLog() {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(adminService.auditLog());
    }

    @GetMapping("/admin/users/{id}/notes")
    public ResponseEntity<?> getNotes(@PathVariable Long id) {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.ok(adminService.notes(id)); }
        catch (IllegalArgumentException ex) { return ResponseEntity.notFound().build(); }
    }

    @PostMapping("/admin/users/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long id, @RequestBody NoteRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addNote(id, request == null ? null : request.getNote(), currentUser())); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage())); }
    }

    @GetMapping("/admin/announcements")
    public ResponseEntity<?> announcements() {
        if (!canReadAdminData()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(adminService.announcements());
    }

    @PostMapping("/admin/announcements")
    public ResponseEntity<?> createAnnouncement(@RequestBody com.kumbukaa.dto.AnnouncementRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAnnouncement(request, currentUser())); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage())); }
        catch (IllegalStateException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", ex.getMessage())); }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> notifications() {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(adminService.notifications(user.getId()));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(@PathVariable Long id) {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            adminService.markNotificationRead(id, user.getId());
            return ResponseEntity.ok(new MessageResponse("notification marked as read"));
        } catch (IllegalArgumentException ex) { return ResponseEntity.notFound().build(); }
    }

    @PatchMapping("/admin/users/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            adminService.changeRole(id, request == null ? null : request.getRole(), currentUser());
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage())); }
        catch (IllegalStateException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", ex.getMessage())); }
    }

    @PatchMapping("/admin/users/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            adminService.setAccountStatus(id, request == null ? null : request.getStatus(), currentUser());
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage())); }
        catch (IllegalStateException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", ex.getMessage())); }
    }

    @PatchMapping("/admin/users/{id}/verification")
    public ResponseEntity<?> changeVerification(@PathVariable Long id, @RequestBody VerificationRequest request) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            adminService.setVerificationStatus(id, request == null ? null : request.getVerificationStatus(), currentUser());
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage())); }
    }

    @PostMapping("/admin/loans/{kind}/{id}/remind")
    public ResponseEntity<?> remindLoan(@PathVariable String kind, @PathVariable Long id) {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.ok(new ReminderResponse(adminService.sendOverdueReminder(id, kind, currentUser()))); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage())); }
        catch (IllegalStateException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", ex.getMessage())); }
    }

    @PostMapping("/admin/reminders/overdue")
    public ResponseEntity<?> remindAllOverdue() {
        if (!isAdmin()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try { return ResponseEntity.ok(new ReminderResponse(adminService.sendOverdueReminders(currentUser()))); }
        catch (IllegalStateException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("message", ex.getMessage())); }
    }

    public static class RoleRequest {
        private String role;
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
    public static class StatusRequest {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    public static class VerificationRequest {
        private String verificationStatus;
        public String getVerificationStatus() { return verificationStatus; }
        public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    }
    public static class NoteRequest {
        private String note;
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
    public record ReminderResponse(int sent) {}

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof User user ? user : null;
    }
}
