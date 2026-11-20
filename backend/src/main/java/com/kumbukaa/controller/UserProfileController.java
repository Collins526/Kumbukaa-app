package com.kumbukaa.controller;

import com.kumbukaa.dto.MessageResponse;
import com.kumbukaa.dto.UpdateUserProfileRequest;
import com.kumbukaa.dto.UserProfileResponse;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import com.kumbukaa.util.PhoneNumberUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserRepository userRepository;

    public UserProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public UserProfileResponse getProfile(@AuthenticationPrincipal User user) {
        return toResponse(user);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateUserProfileRequest request) {
        if (request == null
                || request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Name, email, and phone number are required"));
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Enter a valid email address"));
        }
        String phoneNumber = PhoneNumberUtils.normalize(request.getPhoneNumber());
        if (phoneNumber == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Enter a valid phone number"));
        }

        var existingEmail = userRepository.findByEmail(email);
        if (existingEmail.isPresent() && !existingEmail.get().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already registered"));
        }
        var existingPhone = userRepository.findByPhoneNumber(phoneNumber);
        if (existingPhone.isPresent() && !existingPhone.get().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Phone number is already registered"));
        }

        user.setFullName(request.getName().trim());
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber());
    }
}
