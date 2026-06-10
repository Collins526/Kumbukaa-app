package com.kumbukaa.service;

import com.kumbukaa.dto.LoanResponse;
import com.kumbukaa.dto.UserAdminDto;
import com.kumbukaa.entity.User;
import com.kumbukaa.mapper.LoanResponseMapper;
import com.kumbukaa.repository.LoanBorrowedRepository;
import com.kumbukaa.repository.LoanLentRepository;
import com.kumbukaa.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@SuppressWarnings("null")
public class AdminService {

    private final UserRepository userRepository;
    private final LoanLentRepository lentRepository;
    private final LoanBorrowedRepository borrowedRepository;

    public AdminService(UserRepository userRepository, LoanLentRepository lentRepository, LoanBorrowedRepository borrowedRepository) {
        this.userRepository = userRepository;
        this.lentRepository = lentRepository;
        this.borrowedRepository = borrowedRepository;
    }

    public List<UserAdminDto> listAllUsersWithLoans() {
        List<User> users = userRepository.findAll();
        return users.stream().map(user -> {
            List<LoanResponse> lent = lentRepository.findAllByUserId(user.getId()).stream()
                    .map(LoanResponseMapper::toResponse)
                    .collect(Collectors.toList());
            List<LoanResponse> borrowed = borrowedRepository.findAllByUserId(user.getId()).stream()
                    .map(LoanResponseMapper::toResponse)
                    .collect(Collectors.toList());
            return UserAdminDto.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .loansLent(lent)
                    .loansBorrowed(borrowed)
                    .build();
        }).collect(Collectors.toList());
    }

    public User createAdminUser(String fullName, String email, String phoneNumber, String password) {
        if (userRepository.findByEmail(email.toLowerCase().trim()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = User.builder()
                .fullName(fullName.trim())
                .email(email.toLowerCase().trim())
                .phoneNumber(phoneNumber.trim())
                .passwordHash(hashPassword(password))
                .roles("ROLE_ADMIN")
                .build();
        return userRepository.save(user);
    }

    public void resetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(hashPassword(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
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
}
