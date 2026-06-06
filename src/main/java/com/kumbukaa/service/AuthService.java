package com.kumbukaa.service;

import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.LoginWithOtpRequest;
import com.kumbukaa.dto.OtpRequest;
import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.entity.OtpCode;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.OtpCodeRepository;
import com.kumbukaa.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final EmailService emailService;
    private final Random random;

    public AuthService(UserRepository userRepository, OtpCodeRepository otpCodeRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.emailService = emailService;
        this.random = new Random();
    }

    @SuppressWarnings("null")
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getName().trim())
                .email(email)
                .phoneNumber(request.getPhoneNumber().trim())
                .passwordHash(hashPassword(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                "Registration successful",
                createToken(savedUser),
                createRefreshToken(savedUser)
        );
    }

    public AuthResponse login(LoginRequest request) {
        validateLoginRequest(request);

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!hashPassword(request.getPassword()).equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                "Login successful",
                createToken(user),
                createRefreshToken(user)
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
        emailService.sendOtpEmail(email, code);
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

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                "Login with OTP successful",
                createToken(user),
                createRefreshToken(user)
        );
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

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String createToken(User user) {
        String payload = String.format("%d:%s:%d", user.getId(), user.getEmail(), System.currentTimeMillis());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String createRefreshToken(User user) {
        String payload = String.format("refresh:%d:%s:%d", user.getId(), user.getEmail(), System.currentTimeMillis());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
