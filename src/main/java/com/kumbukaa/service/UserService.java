package com.kumbukaa.service;

import com.kumbukaa.dto.UpdateProfileRequest;
import com.kumbukaa.entity.Auth;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.AuthRepository;
import com.kumbukaa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthRepository authRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Update user profile after successful authentication
     * User is identified by their JWT token (passed as userId)
     */
    @SuppressWarnings("null")
    public User updateProfile(Long userId, UpdateProfileRequest request) throws Exception {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");

        // Find user by ID
        Optional<User> userOptional = userRepository.findById(userId);
        User user = userOptional.orElseThrow(() -> new Exception("User not found"));
        
        // Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }

        // Update phone number if provided
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        // Update email if provided (with validation and duplicate check)
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            
            if (!isValidEmail(newEmail)) {
                throw new Exception("Invalid email format");
            }

            // Check if new email already exists (excluding current user's email)
            if (!Objects.equals(user.getEmail(), newEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new Exception("Email already exists");
                }
                // Also check in Auth table
                if (authRepository.findByEmail(newEmail).isPresent()) {
                    throw new Exception("Email already exists");
                }
                user.setEmail(newEmail);
            }
        }

        // Save updated user
        User savedUser = userRepository.save(user);
        User updatedUser = Objects.requireNonNull(savedUser, "Failed to save user");

        // Update Auth record if email or username was changed
        Optional<Auth> authOptional = authRepository.findByUserId(userId);
        if (authOptional.isPresent()) {
            Auth auth = authOptional.orElseThrow(() -> new Exception("Auth record not found"));

            // Update email in Auth if changed
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                auth.setEmail(request.getEmail().trim());
            }
            
            // Update username if provided
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                auth.setUsername(request.getUsername().trim());
            }
            
            authRepository.save(auth);
        }

        return updatedUser;
    }

    /**
     * Get user profile by ID
     */
    public User getUserProfile(Long userId) throws Exception {
        Objects.requireNonNull(userId, "userId is required");
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new Exception("User not found");
        }
        return userOptional.get();
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email) || authRepository.findByEmail(email).isPresent();
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        Objects.requireNonNull(email, "email is required");
        return userRepository.findByEmail(email);
    }


}
