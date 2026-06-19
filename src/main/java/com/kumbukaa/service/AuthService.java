package com.kumbukaa.service;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.LoginWithOtpRequest;
import com.kumbukaa.dto.OtpRequest;
import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.entity.OtpCode;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.OtpCodeRepository;
import com.kumbukaa.event.OtpRequestedEvent;
import com.kumbukaa.repository.UserRepository;
import com.kumbukaa.util.PhoneNumberUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoanClaimService loanClaimService;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, OtpCodeRepository otpCodeRepository, EmailService emailService, JwtTokenProvider jwtTokenProvider, LoanClaimService loanClaimService, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.emailService = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loanClaimService = loanClaimService;
        this.eventPublisher = eventPublisher;
        this.random = new Random();
    }

    @SuppressWarnings("null")
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String normalizedPhone = PhoneNumberUtils.normalize(request.getPhoneNumber());
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (userRepository.findByPhoneNumber(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        User user = User.builder()
            .fullName(request.getName().trim())
            .email(email)
            .phoneNumber(normalizedPhone)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .roles("ROLE_USER")
            .mustChangePassword(false)
            .build();

        User savedUser = userRepository.save(user);
        loanClaimService.claimCounterpartyLoans(savedUser);
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                "Registration successful",
                jwtTokenProvider.createAccessToken(savedUser),
                jwtTokenProvider.createRefreshToken(savedUser)
        );
    }

    public AuthResponse login(LoginRequest request) {
        validateLoginRequest(request);

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!verifyPasswordAndMigrate(user, request.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        loanClaimService.claimCounterpartyLoans(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                "Login successful",
                jwtTokenProvider.createAccessToken(user),
                jwtTokenProvider.createRefreshToken(user)
        );
    }

    public AuthResponse adminLogin(LoginRequest request) {
        validateLoginRequest(request);

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!verifyPasswordAndMigrate(user, request.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Verify user has admin role
        if (user.getRoles() == null || !user.getRoles().contains("ROLE_ADMIN")) {
            throw new IllegalArgumentException("User is not authorized as an admin");
        }

        String message = "Admin login successful";
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            message = "Admin login successful - password change required";
        }

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                message,
                jwtTokenProvider.createAccessToken(user),
                jwtTokenProvider.createRefreshToken(user)
        );
    }

    @SuppressWarnings("null")
    public String requestOtp(OtpRequest request) {
        String email = validateEmail(request.getEmail());

        userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email is not registered"));

        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        OtpCode otpCode = OtpCode.builder()
                .email(email)
                .code(code)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        otpCodeRepository.save(otpCode);
        eventPublisher.publishEvent(new OtpRequestedEvent(email, code));
        return "OTP has been sent to the email.";
    }

    public AuthResponse loginWithOtp(LoginWithOtpRequest request) {
        String email = validateEmail(request.getEmail());
        String code = validateCode(request.getCode());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email is not registered"));

        Optional<OtpCode> existingOtp = otpCodeRepository.findFirstByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(email, code);
        if (existingOtp.isEmpty()) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        OtpCode otpCode = existingOtp.get();
        if (otpCode.getExpiresAt() == null || otpCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP code has expired");
        }

        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);

        loanClaimService.claimCounterpartyLoans(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                "Login with OTP successful",
                jwtTokenProvider.createAccessToken(user),
                jwtTokenProvider.createRefreshToken(user)
        );
    }

    /**
     * Verifies a raw password against the stored password hash.
     * Supports BCrypt hashes and legacy SHA-256 hex hashes. When a legacy
     * hash is detected and the password verifies, the stored password is
     * migrated to BCrypt.
     */
    private boolean verifyPasswordAndMigrate(User user, String rawPassword) {
        String stored = user.getPasswordHash();
        if (stored == null) return false;

        // If BCrypt
        try {
            if (passwordEncoder.matches(rawPassword, stored)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // Fallback: check legacy SHA-256 hex
        String sha = computeSha256Hex(rawPassword);
        if (sha.equalsIgnoreCase(stored)) {
            // migrate to BCrypt
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null
                || request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()
                || request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
            throw new IllegalArgumentException("Name, email, phone number, password, and password confirmation are required");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password must match");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }
    }

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("OTP code is required");
        }
        return code.trim();
    }

    private String computeSha256Hex(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashedBytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

}
