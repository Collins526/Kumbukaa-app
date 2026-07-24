package com.kumbukaa.service;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.ForgotPasswordRequest;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.LoginWithOtpRequest;
import com.kumbukaa.dto.OtpRequest;
import com.kumbukaa.dto.PasswordResetResponse;
import com.kumbukaa.dto.VerifyOtpResponse;
import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.dto.ResetPasswordRequest;
import com.kumbukaa.dto.VerifyOtpRequest;
import com.kumbukaa.entity.OtpCode;
import com.kumbukaa.entity.PasswordResetOtp;
import com.kumbukaa.entity.PasswordResetToken;
import com.kumbukaa.entity.User;
import com.kumbukaa.event.OtpRequestedEvent;
import com.kumbukaa.repository.OtpCodeRepository;
import com.kumbukaa.repository.PasswordResetOtpRepository;
import com.kumbukaa.repository.PasswordResetTokenRepository;
import com.kumbukaa.repository.UserRepository;
import com.kumbukaa.util.PhoneNumberUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoanClaimService loanClaimService;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;
    private final Random random;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
                       OtpCodeRepository otpCodeRepository,
                       PasswordResetOtpRepository passwordResetOtpRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       JwtTokenProvider jwtTokenProvider,
                       LoanClaimService loanClaimService,
                       ApplicationEventPublisher eventPublisher,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loanClaimService = loanClaimService;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
        this.random = new SecureRandom();
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
     * Sends a password reset OTP to a registered user without revealing whether the account exists.
     */
    @Transactional
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        String email = validateEmail(request.getEmail());

        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            List<PasswordResetOtp> activeOtps = passwordResetOtpRepository.findAllByUserAndUsedFalseAndVerifiedFalseOrderByCreatedAtDesc(user);
            for (PasswordResetOtp otp : activeOtps) {
                otp.setUsed(true);
                passwordResetOtpRepository.save(otp);
            }

            String otp = generateSixDigitOtp();
            String hashedOtp = hashOtp(otp);
            PasswordResetOtp passwordResetOtp = PasswordResetOtp.builder()
                    .user(user)
                    .hashedOtp(hashedOtp)
                    .expiryTime(LocalDateTime.now().plusMinutes(10))
                    .attempts(0)
                    .verified(false)
                    .used(false)
                    .build();
            passwordResetOtpRepository.save(passwordResetOtp);
            emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getFullName(), otp);
            return new PasswordResetResponse("a verification code has been sent");
        }

        return new PasswordResetResponse("account does not exist");
    }

    /**
     * Verifies a password reset OTP and returns a short-lived reset token when valid.
     */
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String email = validateEmail(request.getEmail());
        String otp = validateCode(request.getOtp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or OTP"));

        Optional<PasswordResetOtp> latestOtp = passwordResetOtpRepository.findAllByUserAndUsedFalse(user).stream()
                .filter(candidate -> !Boolean.TRUE.equals(candidate.getUsed()))
                .filter(candidate -> !Boolean.TRUE.equals(candidate.getVerified()))
                .findFirst();

        if (latestOtp.isEmpty()) {
            throw new IllegalArgumentException("No active OTP found");
        }

        PasswordResetOtp passwordResetOtp = latestOtp.get();
        if (passwordResetOtp.getExpiryTime() == null || passwordResetOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP has expired");
        }

        if (passwordResetOtp.getAttempts() >= 5) {
            throw new IllegalStateException("Too many failed OTP attempts");
        }

        if (!passwordEncoder.matches(otp, passwordResetOtp.getHashedOtp())) {
            passwordResetOtp.setAttempts(passwordResetOtp.getAttempts() + 1);
            passwordResetOtpRepository.save(passwordResetOtp);
            throw new IllegalArgumentException("Invalid OTP");
        }

        passwordResetOtp.setVerified(true);
        passwordResetOtpRepository.save(passwordResetOtp);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        return new VerifyOtpResponse(token, "OTP verified successfully.");
    }

    /**
     * Resets the user's password using a valid, unexpired reset token.
     */
    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getResetToken() == null || request.getResetToken().isBlank()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password must match");
        }
        validatePasswordPolicy(request.getNewPassword());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getResetToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));
        if (resetToken.getExpiryTime() == null || resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        List<PasswordResetOtp> activeOtps = passwordResetOtpRepository.findAllByUserAndUsedFalse(user);
        for (PasswordResetOtp otp : activeOtps) {
            otp.setUsed(true);
            passwordResetOtpRepository.save(otp);
        }

        return new PasswordResetResponse("Password reset successfully.");
    }

    private String generateSixDigitOtp() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String hashOtp(String otp) {
        return passwordEncoder.encode(otp);
    }

    private void validatePasswordPolicy(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
        if (!Pattern.compile("[^A-Za-z0-9]").matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
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
