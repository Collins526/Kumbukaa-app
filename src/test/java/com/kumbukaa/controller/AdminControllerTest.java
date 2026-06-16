package com.kumbukaa.controller;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.UserSummaryDto;
import com.kumbukaa.entity.User;
import com.kumbukaa.service.AdminService;
import com.kumbukaa.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        System.clearProperty("app.admin-emails");
    }

    @Test
    void getAllUsersWithLoans_allowsAdmin() {
        AdminService svc = mock(AdminService.class);
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        when(svc.listAllUsersWithLoans()).thenReturn(List.of(new UserSummaryDto()));

        AdminController controller = new AdminController(svc, authService, tokenProvider);
        User admin = User.builder().id(5L).email("admin@example.com").roles("ROLE_ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                admin, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));

        var resp = controller.getAllUsersWithLoans();
        assertEquals(200, resp.getStatusCode().value());
        verify(svc).listAllUsersWithLoans();
    }

    @Test
    void getAllUsersWithLoans_forbidsNonAdmin() {
        AdminService svc = mock(AdminService.class);
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AdminController controller = new AdminController(svc, authService, tokenProvider);
        User user = User.builder().id(6L).email("user@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                user, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));

        var resp = controller.getAllUsersWithLoans();
        assertEquals(403, resp.getStatusCode().value());
        verifyNoInteractions(svc);
    }

    @Test
    void createAdminUser_returnsCreatedAndToken() {
        AdminService svc = mock(AdminService.class);
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AdminController controller = new AdminController(svc, authService, tokenProvider);

        var request = new com.kumbukaa.dto.AdminCreateRequest(
                "Admin User",
                "admin@example.com",
                "+254700000000",
                "SecurePassword123",
                "SecurePassword123");

        User created = User.builder()
                .id(99L)
                .fullName("Admin User")
                .email("admin@example.com")
                .phoneNumber("+254700000000")
                .build();

        when(svc.createAdminUser("Admin User", "admin@example.com", "+254700000000", "SecurePassword123"))
                .thenReturn(created);
        when(tokenProvider.createAccessToken(created)).thenReturn("access-token-abc");
        when(tokenProvider.createRefreshToken(created)).thenReturn("refresh-token-xyz");

        var resp = controller.createAdminUser(request);
        assertEquals(201, resp.getStatusCode().value());
        var body = java.util.Objects.requireNonNull(resp.getBody());
        assertEquals("access-token-abc", body.getToken());
        assertEquals("refresh-token-xyz", body.getRefreshToken());
        verify(svc).createAdminUser("Admin User", "admin@example.com", "+254700000000", "SecurePassword123");
    }

    @Test
    void createAdminUser_invalidRequest_returnsBadRequest() {
        AdminService svc = mock(AdminService.class);
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AdminController controller = new AdminController(svc, authService, tokenProvider);

        var request = new com.kumbukaa.dto.AdminCreateRequest(
                "",
                "",
                "",
                "",
                "");

        var resp = controller.createAdminUser(request);
        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(svc);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void resetUserPassword_returnsNewPasswordAndMessage() {
        AdminService svc = mock(AdminService.class);
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AdminController controller = new AdminController(svc, authService, tokenProvider);

        User admin = User.builder().id(5L).email("admin@example.com").roles("ROLE_ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                admin, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));

        var request = new AdminController.ResetPasswordRequest();
        request.setPassword("NewPass123");

        var resp = controller.resetUserPassword(1L, request);
        assertEquals(200, resp.getStatusCode().value());
        var body = java.util.Objects.requireNonNull(resp.getBody());
        assertEquals("New password created", body.getMessage());
        assertEquals("NewPass123", body.getPassword());
        verify(svc).resetUserPassword(1L, "NewPass123");
    }
}
