package com.kumbukaa.service;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.ForgotPasswordRequest;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.ResetPasswordRequest;
import com.kumbukaa.dto.VerifyOtpRequest;
import com.kumbukaa.entity.PasswordResetOtp;
import com.kumbukaa.entity.PasswordResetToken;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.OtpCodeRepository;
import com.kumbukaa.repository.PasswordResetOtpRepository;
import com.kumbukaa.repository.PasswordResetTokenRepository;
import com.kumbukaa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServicePasswordResetTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private LoanClaimService loanClaimService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EmailService emailService;

    @Mock
    private TwilioSmsService twilioSmsService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(
                userRepository,
                otpCodeRepository,
                passwordResetOtpRepository,
                passwordResetTokenRepository,
                jwtTokenProvider,
                loanClaimService,
                eventPublisher,
                emailService,
                twilioSmsService
        );
    }

    @Test
    void forgotPassword_shouldCreateOtpAndSendEmail_whenUserExists() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Jane Doe")
                .passwordHash("hash")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetOtpRepository.findAllByUserAndUsedFalseAndVerifiedFalseOrderByCreatedAtDesc(user)).thenReturn(List.of());
        when(passwordResetOtpRepository.save(any(PasswordResetOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));

        assertEquals("a verification code has been sent", response.getMessage());
        verify(emailService).sendPasswordResetOtpEmail(eq("user@example.com"), eq("Jane Doe"), anyString());

        ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(passwordResetOtpRepository).save(otpCaptor.capture());
        assertNotNull(otpCaptor.getValue().getHashedOtp());
        assertFalse(otpCaptor.getValue().getUsed());
    }

    @Test
    void forgotPassword_shouldCreateOtpAndSendSms_whenPhoneNumberMatchesUser() {
        User user = User.builder()
                .id(3L)
                .email("user@example.com")
                .fullName("Jane Doe")
                .phoneNumber("254700000000")
                .build();
        when(userRepository.findAllByNormalizedPhoneNumber("254700000000")).thenReturn(List.of(user));
        when(passwordResetOtpRepository.findAllByUserAndUsedFalseAndVerifiedFalseOrderByCreatedAtDesc(user))
                .thenReturn(List.of());
        when(passwordResetOtpRepository.save(any(PasswordResetOtp.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.forgotPassword(
                new ForgotPasswordRequest(null, "+254 700 000 000"));

        assertEquals("a verification code has been sent by SMS", response.getMessage());
        verify(twilioSmsService).sendPasswordResetOtpSms(eq("254700000000"), anyString());
        verify(emailService, never()).sendPasswordResetOtpEmail(anyString(), anyString(), anyString());
    }

    @Test
    void verifyOtp_shouldFindAccountByPhoneNumber() {
        User user = User.builder()
                .id(4L)
                .email("user@example.com")
                .phoneNumber("254700000000")
                .build();
        PasswordResetOtp otp = PasswordResetOtp.builder()
                .user(user)
                .hashedOtp(new BCryptPasswordEncoder().encode("123456"))
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .attempts(0)
                .verified(false)
                .used(false)
                .build();
        when(userRepository.findAllByNormalizedPhoneNumber("254700000000")).thenReturn(List.of(user));
        when(passwordResetOtpRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of(otp));

        var response = authService.verifyOtp(
                new VerifyOtpRequest(null, "+254 700 000 000", "123456"));

        assertEquals("OTP verified successfully.", response.getMessage());
        assertTrue(otp.getVerified());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndInvalidateOtp_whenTokenIsValid() {
        User user = User.builder()
                .id(2L)
                .email("user@example.com")
                .fullName("Jane Doe")
                .passwordHash("old-hash")
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .id(10L)
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        PasswordResetOtp otp = PasswordResetOtp.builder()
                .id(99L)
                .user(user)
                .hashedOtp("hashed")
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .attempts(0)
                .verified(true)
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse(token.getToken())).thenReturn(Optional.of(token));
        when(passwordResetOtpRepository.findAllByUserAndUsedFalse(user)).thenReturn(List.of(otp));
        when(passwordResetOtpRepository.save(any(PasswordResetOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.resetPassword(new ResetPasswordRequest(token.getToken(), "Password@123", "Password@123"));

        assertEquals("Password reset successfully.", response.getMessage());
        assertTrue(token.getUsed());
        assertTrue(otp.getUsed());
        verify(passwordResetTokenRepository).save(token);
        verify(passwordResetOtpRepository, atLeastOnce()).save(any(PasswordResetOtp.class));
    }

    @Test
    void login_rejectsSuspendedAccount() {
        User user = User.builder().id(10L).email("blocked@example.com")
                .accountStatus("SUSPENDED")
                .passwordHash(new BCryptPasswordEncoder().encode("Password@123")).build();
        when(userRepository.findByEmail("blocked@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("blocked@example.com", "Password@123"), "test-device"));
        verify(loanClaimService, never()).claimCounterpartyLoans(any());
        verify(userRepository, never()).save(user);
    }

    @Test
    void login_persistsSignInTimeAndUserAgent() {
        User user = User.builder().id(11L).email("active@example.com")
                .passwordHash(new BCryptPasswordEncoder().encode("Password@123")).build();
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(user)).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(user)).thenReturn("refresh");

        authService.login(new LoginRequest("active@example.com", "Password@123"), "browser-test");

        assertNotNull(user.getLastSeenAt());
        assertEquals("browser-test", user.getLastDevice());
        verify(userRepository).save(user);
    }
}
