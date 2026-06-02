package com.kumbukaa.service;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.BorrowerDTO;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.entity.Auth;
import com.kumbukaa.entity.Lender;
import com.kumbukaa.entity.User;
 
import com.kumbukaa.repository.AuthRepository;
import com.kumbukaa.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private LenderService lenderService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.resend.from-email:noreply@kumbukaa.com}")
    private String resendFromEmail;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpDetails> otpStore = new ConcurrentHashMap<>();

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public AuthResponse register(RegisterRequest request) throws Exception {
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        if (email == null || email.isEmpty()) {
            throw new Exception("Email is required");
        }
        if (!isValidEmail(email)) {
            throw new Exception("Invalid email");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new Exception("Password is required");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new Exception("Passwords do not match");
        }

        if (authRepository.findByEmail(email).isPresent()) {
            throw new Exception("Email already exists");
        }

        String phoneNumber = request.getPhoneNumber() == null ? null : request.getPhoneNumber().trim();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new Exception("Phone number is required");
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new Exception("Phone number already exists");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(email);
        newUser.setPhoneNumber(phoneNumber);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(newUser);

        Auth auth = new Auth();
        auth.setUser(savedUser);
        auth.setPassword(passwordEncoder.encode(request.getPassword()));
        auth.setEmail(email);
        auth.setUsername(email); // Set username to email
        auth.setIsVerified(true);
        auth.setIsActive(true);
        authRepository.save(auth);

        ensureBorrowerRecord(savedUser);
        ensureLenderRecord(savedUser);

        String token = jwtTokenProvider.generateToken(email, savedUser.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(email, savedUser.getId());
        auth.setToken(token);
        auth.setRefreshToken(refreshToken);
        authRepository.save(auth);

        AuthResponse response = new AuthResponse();
        response.setId(auth.getId());
        response.setUserId(savedUser.getId());
        response.setEmail(email);
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setTokenExpiration(jwtTokenProvider.getTokenExpiration(token));
        response.setIsVerified(auth.getIsVerified());
        response.setMessage("User registered successfully");

        return response;
    }

    private void ensureBorrowerRecord(User savedUser) {
        if (borrowerService.findByUserId(savedUser.getId()).isPresent()) {
            return;
        }

        BorrowerDTO borrowerDTO = new BorrowerDTO();
        borrowerDTO.setUserId(savedUser.getId());
        borrowerDTO.setCreditScore(600);
        borrowerDTO.setIdNumber("ID-" + savedUser.getId());
        borrowerDTO.setEmploymentStatus("EMPLOYED");
        borrowerDTO.setAnnualIncome(0.0);
        borrowerDTO.setDateOfBirth(LocalDate.of(1990, 1, 1));
        borrowerDTO.setAddress("Unknown");
        borrowerDTO.setCity("Unknown");
        borrowerDTO.setPostalCode("00000");
        borrowerDTO.setIsVerified(false);
        borrowerDTO.setTotalBorrowed(0.0);
        borrowerDTO.setTotalRepaid(0.0);
        borrowerService.createBorrower(borrowerDTO);
    }

    private void ensureLenderRecord(User savedUser) {
        if (lenderService.findByUserId(savedUser.getId()).isPresent()) {
            return;
        }

        Lender lender = new Lender();
        lender.setUser(savedUser);
        lender.setLenderName(savedUser.getName());
        lender.setAvailableCapital(0.0);
        lender.setInterestRate(5.0);
        lender.setDateOfBirth(LocalDate.of(1990, 1, 1));
        lender.setAddress("Unknown");
        lender.setCity("Unknown");
        lender.setPostalCode("00000");
        lender.setIsVerified(true);
        lender.setTotalLent(0.0);
        lender.setTotalRecovered(0.0);
        lenderService.createLender(lender);
    }

    public AuthResponse login(LoginRequest request) throws Exception {
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        if (email == null || email.isEmpty()) {
            throw new Exception("Email is required");
        }
        if (!isValidEmail(email)) {
            throw new Exception("Invalid email");
        }

        Optional<Auth> authOptional = authRepository.findByEmail(email);
        if (authOptional.isEmpty()) {
            throw new Exception("Email does not exist");
        }

        Auth auth = authOptional.get();

        if (!auth.getIsActive()) {
            throw new Exception("Account is inactive");
        }

        boolean authenticated = false;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            authenticated = passwordEncoder.matches(request.getPassword(), auth.getPassword());
        } else if (request.getOtp() != null && !request.getOtp().trim().isEmpty()) {
            authenticated = validateOtp(request.getEmail().trim(), request.getOtp().trim());
        } else {
            throw new Exception("Password or OTP is required");
        }

        if (!authenticated) {
            throw new Exception("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(auth.getEmail(), auth.getUser().getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(auth.getEmail(), auth.getUser().getId());

        auth.setToken(token);
        auth.setRefreshToken(refreshToken);
        auth.setLastLogin(LocalDateTime.now());
        Auth updatedAuth = authRepository.save(auth);

        AuthResponse response = new AuthResponse();
        response.setId(updatedAuth.getId());
        response.setUserId(updatedAuth.getUser().getId());
        response.setEmail(updatedAuth.getEmail());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setTokenExpiration(jwtTokenProvider.getTokenExpiration(token));
        response.setIsVerified(updatedAuth.getIsVerified());
        response.setMessage("Login successful");

        return response;
    }

    public String requestOtp(String email) throws Exception {
        String normalizedEmail = email == null ? null : email.trim();
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new Exception("Email is required");
        }
        if (!isValidEmail(normalizedEmail)) {
            throw new Exception("Invalid email");
        }

        Optional<Auth> authOptional = authRepository.findByEmail(normalizedEmail);
        if (authOptional.isEmpty()) {
            throw new Exception("Email does not exist");
        }

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        otpStore.put(normalizedEmail.toLowerCase(), new OtpDetails(otp, expiresAt));
        try {
            sendOtpByEmail(normalizedEmail, otp);
            return "OTP request accepted. Check your email for the code.";
        } catch (Exception e) {
            otpStore.remove(normalizedEmail.toLowerCase());
            throw new Exception("Unable to send OTP email: " + e.getMessage(), e);
        }
    }

    private boolean validateOtp(String email, String otp) {
        if (email == null || otp == null) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        OtpDetails details = otpStore.get(normalizedEmail);
        if (details == null || details.isExpired() || !details.getCode().equals(otp)) {
            return false;
        }

        otpStore.remove(normalizedEmail);
        return true;
    }

    private void sendOtpByEmail(String email, String otp) throws Exception {
        String subject = "Kumbukaa - One Time Password (OTP)";
        String body = "Your OTP for Kumbukaa Lending App is: " + otp + "\n\n"
                + "This OTP will expire in 10 minutes.\n"
                + "Do not share this code with anyone.\n\n"
                + "If you did not request this code, please ignore this email.";

        if (resendApiKey != null && !resendApiKey.isBlank()) {
            sendEmailWithResend(email, subject, body);
        } else if (mailSender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom(mailFrom);
            mailSender.send(message);
        } else {
            throw new IllegalStateException("No email delivery provider configured");
        }
        System.out.println("[OTP] Email sent successfully to: " + email);
    }

    private void sendEmailWithResend(String email, String subject, String body) throws Exception {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is not configured");
        }
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiKey = Objects.requireNonNull(resendApiKey, "resendApiKey is required");
        String fromEmail = Objects.requireNonNull(resendFromEmail, "resendFromEmail is required");

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", fromEmail);
        payload.put("to", email);
        payload.put("subject", subject);
        payload.put("text", body);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(payload);
        String payloadJson = Objects.requireNonNull(json, "payload json must not be null");

        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);
        HttpMethod method = Objects.requireNonNull(HttpMethod.POST);
        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.resend.com/emails",
            method,
            request,
            String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Resend email failed: " + response.getStatusCode() + " " + response.getBody());
        }
    }

    private static class OtpDetails {
        private final String code;
        private final LocalDateTime expiresAt;

        public OtpDetails(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        public String getCode() {
            return code;
        }

        public boolean isExpired() {
            return expiresAt.isBefore(LocalDateTime.now());
        }
    }

    public AuthResponse refreshToken(String refreshToken) throws Exception {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new Exception("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        Optional<Auth> authOptional = authRepository.findByEmail(email);
        if (authOptional.isEmpty()) {
            throw new Exception("User not found");
        }

        Auth auth = authOptional.get();

        
        String newToken = jwtTokenProvider.generateToken(email, userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email, userId);

        
        auth.setToken(newToken);
        auth.setRefreshToken(newRefreshToken);
        Auth updatedAuth = authRepository.save(auth);

        
        AuthResponse response = new AuthResponse();
        response.setId(updatedAuth.getId());
        response.setUserId(updatedAuth.getUser().getId());
        response.setEmail(updatedAuth.getEmail());
        response.setToken(newToken);
        response.setRefreshToken(newRefreshToken);
        response.setTokenExpiration(jwtTokenProvider.getTokenExpiration(newToken));
        response.setIsVerified(updatedAuth.getIsVerified());
        response.setMessage("Token refreshed successfully");

        return response;
    }

    public void logout(Long authId) throws Exception {
        if (authId == null) {
            throw new Exception("Auth ID is required");
        }
        Optional<Auth> authOptional = authRepository.findById(authId);
        if (authOptional.isEmpty()) {
            throw new Exception("Auth record not found");
        }

        Auth auth = authOptional.get();
        auth.setToken(null);
        auth.setRefreshToken(null);
        authRepository.save(auth);
    }

    public Auth createAuth(Auth auth) {
        if (auth == null) {
            throw new IllegalArgumentException("Auth record cannot be null");
        }
        return authRepository.save(auth);
    }

    public Optional<Auth> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return authRepository.findById(id);
    }

    public Optional<Auth> findByEmail(String email) {
        return authRepository.findByEmail(email);
    }

    public Optional<Auth> findByUserId(Long userId) {
        return authRepository.findByUserId(userId);
    }

    public List<Auth> findAll() {
        return authRepository.findAll();
    }

    public Auth updateAuth(Auth auth) {
        if (auth == null) {
            throw new IllegalArgumentException("Auth record cannot be null");
        }
        return authRepository.save(auth);
    }

    public void deleteAuth(Long id) {
        if (id != null) {
            authRepository.deleteById(id);
        }
    }

    public boolean verifyAuth(Long id) {
        if (id == null) return false;
        Optional<Auth> authOptional = authRepository.findById(id);
        if (authOptional.isPresent()) {
            Auth auth = authOptional.get();
            auth.setIsVerified(true);
            authRepository.save(auth);
            return true;
        }
        return false;
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token) && !jwtTokenProvider.isTokenExpired(token);
    }

    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    public Long getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
