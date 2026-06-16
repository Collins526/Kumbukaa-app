package com.kumbukaa.service;

import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class AdminServiceTest {

    @AfterEach
    void tearDown() {
        // clear system properties
        System.clearProperty("app.admin-emails");
    }

    @Test
    void listAllUsersWithLoans_returnsMappedUsers() {
        UserRepository userRepository = mock(UserRepository.class);

        User user = User.builder()
                .id(1L)
                .fullName("Alice")
                .email("alice@example.com")
                .phoneNumber("+254700000001")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user));

        AdminService svc = new AdminService(userRepository);

        List<?> results = svc.listAllUsersWithLoans();
        assertNotNull(results);
        assertEquals(1, results.size());
        Object first = results.get(0);
        assertTrue(first instanceof com.kumbukaa.dto.UserSummaryDto);
        com.kumbukaa.dto.UserSummaryDto dto = (com.kumbukaa.dto.UserSummaryDto) first;
        assertEquals(1L, dto.getId());
        assertEquals("Alice", dto.getFullName());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("+254700000001", dto.getPhoneNumber());
    }

    @Test
    void resetUserPassword_hashesAndSaves() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);

        User existing = User.builder().id(2L).email("bob@example.com").passwordHash("oldhash").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));

        AdminService svc = new AdminService(userRepository);
        svc.resetUserPassword(2L, "newpass123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(2L, saved.getId());
        String expected = sha256Hex("newpass123");
        assertEquals(expected, saved.getPasswordHash());
    }

    @Test
    void createAdminUser_persistsAdminRole() {
        UserRepository userRepository = mock(UserRepository.class);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        AdminService svc = new AdminService(userRepository);
        svc.createAdminUser("Admin User", "admin@example.com", "+254700000000", "SecurePassword123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Admin User", saved.getFullName());
        assertEquals("admin@example.com", saved.getEmail());
        assertTrue(saved.getRoles().contains("ROLE_ADMIN"));
    }

    private String sha256Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
