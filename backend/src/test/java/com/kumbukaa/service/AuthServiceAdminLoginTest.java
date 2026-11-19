package com.kumbukaa.service;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.OtpCodeRepository;
import com.kumbukaa.repository.PasswordResetOtpRepository;
import com.kumbukaa.repository.PasswordResetTokenRepository;
import com.kumbukaa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceAdminLoginTest {

    @Test
    void adminLoginDoesNotPromoteFormerlyDesignatedEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        String email = "collinskibet526@gmail.com";
        User user = User.builder()
                .id(2L)
                .email(email)
                .passwordHash(new BCryptPasswordEncoder().encode("valid-password"))
                .roles("ROLE_USER")
                .accountStatus("ACTIVE")
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        AuthService authService = new AuthService(
                userRepository,
                mock(OtpCodeRepository.class),
                mock(PasswordResetOtpRepository.class),
                mock(PasswordResetTokenRepository.class),
                mock(JwtTokenProvider.class),
                mock(LoanClaimService.class),
                mock(ApplicationEventPublisher.class),
                mock(EmailService.class));
        LoginRequest request = new LoginRequest(email, "valid-password");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> authService.adminLogin(request));

        assertEquals("User is not authorized as an admin", exception.getMessage());
        assertEquals("ROLE_USER", user.getRoles());
        verify(userRepository, never()).save(user);
    }
}
