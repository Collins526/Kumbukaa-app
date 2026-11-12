package com.kumbukaa.config;

import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminSeederTest {

    @Test
    void runPreservesExistingAdminPassword() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        String passwordHash = new BCryptPasswordEncoder().encode("existing-password");
        User existing = User.builder()
                .id(1L)
                .email("admin@example.com")
                .passwordHash(passwordHash)
                .roles("ROLE_ADMIN")
                .mustChangePassword(false)
                .build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        createSeeder(userRepository).run(new DefaultApplicationArguments(new String[0]));

        assertEquals(passwordHash, existing.getPasswordHash());
        assertFalse(existing.getMustChangePassword());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void runCreatesAdminThatMustChangeDefaultPassword() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createSeeder(userRepository).run(new DefaultApplicationArguments(new String[0]));

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertEquals("ROLE_ADMIN", created.getRoles());
        assertTrue(Boolean.TRUE.equals(created.getMustChangePassword()));
        assertTrue(new BCryptPasswordEncoder().matches("temporary-password", created.getPasswordHash()));
    }

    @Test
    void runRequiresPasswordChangeWhenExistingAdminStillUsesDefaultPassword() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        String passwordHash = new BCryptPasswordEncoder().encode("temporary-password");
        User existing = User.builder()
                .id(1L)
                .email("admin@example.com")
                .passwordHash(passwordHash)
                .roles("ROLE_ADMIN")
                .mustChangePassword(false)
                .build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        createSeeder(userRepository).run(new DefaultApplicationArguments(new String[0]));

        assertEquals(passwordHash, existing.getPasswordHash());
        assertTrue(existing.getMustChangePassword());
        verify(userRepository).save(existing);
    }

    private AdminSeeder createSeeder(UserRepository userRepository) throws Exception {
        AdminSeeder seeder = new AdminSeeder(userRepository);
        setField(seeder, "defaultAdminEmail", "admin@example.com");
        setField(seeder, "defaultAdminPassword", "temporary-password");
        setField(seeder, "defaultAdminPhone", "+254700000000");
        return seeder;
    }

    private void setField(AdminSeeder seeder, String name, String value) throws Exception {
        Field field = AdminSeeder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(seeder, value);
    }
}
