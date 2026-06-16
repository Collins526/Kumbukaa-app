package com.kumbukaa.service;

import com.kumbukaa.dto.UserSummaryDto;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@SuppressWarnings("null")
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserSummaryDto> listAllUsersWithLoans() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::toUserSummaryDto).collect(Collectors.toList());
    }

    public UserSummaryDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserSummaryDto(user);
    }

    private UserSummaryDto toUserSummaryDto(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public User createAdminUser(String fullName, String email, String phoneNumber, String password) {
        if (userRepository.findByEmail(email.toLowerCase().trim()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = User.builder()
                .fullName(fullName.trim())
                .email(email.toLowerCase().trim())
                .phoneNumber(phoneNumber.trim())
                .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password))
                .roles("ROLE_ADMIN")
                .build();
        return userRepository.save(user);
    }

    public void resetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
    }

    
}
