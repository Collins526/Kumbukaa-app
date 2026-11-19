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
    void getAllUsersWithLoans_allowsAdminAuthorityWithoutPrefix() {
        AdminService svc = mock(AdminService.class);
        AdminController controller = new AdminController(svc, mock(AuthService.class), mock(JwtTokenProvider.class));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        User.builder().id(5L).email("admin@example.com").roles("ADMIN").build(),
                        null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ADMIN"))));
        when(svc.listAllUsersWithLoans()).thenReturn(List.of(new UserSummaryDto()));

        assertEquals(200, controller.getAllUsersWithLoans().getStatusCode().value());
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
    void formerDesignatedEmailDoesNotGrantAdminDataAccess() {
        AdminService svc = mock(AdminService.class);
        AdminController controller = new AdminController(svc, mock(AuthService.class), mock(JwtTokenProvider.class));
        User user = User.builder()
                .id(6L)
                .email("collinskibet526@gmail.com")
                .roles("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));

        assertEquals(403, controller.getAdminProfile().getStatusCode().value());
        assertEquals(403, controller.getAllUsersWithLoans().getStatusCode().value());
        verifyNoInteractions(svc);
    }

    @Test
    void supportCanReadAdminProfileAndUserListButCannotCreateAdmins() {
        AdminService svc = mock(AdminService.class);
        AdminController controller = new AdminController(svc, mock(AuthService.class), mock(JwtTokenProvider.class));
        User support = User.builder().id(8L).email("support@example.com").fullName("Support").roles("ROLE_SUPPORT_AGENT").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                support, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPPORT_AGENT"))));
        when(svc.listAllUsersWithLoans()).thenReturn(List.of(new UserSummaryDto()));

        assertEquals(200, controller.getAdminProfile().getStatusCode().value());
        assertEquals("SUPPORT", controller.getAdminProfile().getBody().getAccessRole());
        assertEquals(200, controller.getAllUsersWithLoans().getStatusCode().value());
        var createResponse = controller.createAdminUser(new com.kumbukaa.dto.AdminCreateRequest(
                "Another Admin", "new@example.com", "+254700000000", "SecurePassword123", "SecurePassword123"));
        assertEquals(403, createResponse.getStatusCode().value());
        verify(svc).listAllUsersWithLoans();
        verifyNoMoreInteractions(svc);
    }

    @Test
    void createAdminUser_returnsCreatedAndToken() {
        AdminService svc = mock(AdminService.class);
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        AdminController controller = new AdminController(svc, authService, tokenProvider);
        authenticateAsAdmin();

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
        authenticateAsAdmin();

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
    void createAdminUser_forbidsNonAdmin() {
        AdminService svc = mock(AdminService.class);
        AdminController controller = new AdminController(svc, mock(AuthService.class), mock(JwtTokenProvider.class));
        User user = User.builder().id(6L).email("user@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));

        var request = new com.kumbukaa.dto.AdminCreateRequest(
                "Admin User",
                "admin@example.com",
                "+254700000000",
                "SecurePassword123",
                "SecurePassword123");

        var response = controller.createAdminUser(request);
        assertEquals(403, response.getStatusCode().value());
        verifyNoInteractions(svc);
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

    @Test
    void deleteUser_refusesToDeleteSignedInAdmin() {
        AdminService svc = mock(AdminService.class);
        AdminController controller = new AdminController(svc, mock(AuthService.class), mock(JwtTokenProvider.class));
        authenticateAsAdmin();

        var response = controller.deleteUser(5L);

        assertEquals(409, response.getStatusCode().value());
        verifyNoInteractions(svc);
    }

    private void authenticateAsAdmin() {
        User admin = User.builder().id(5L).email("admin@example.com").roles("ROLE_ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
