package com.kumbukaa.service;

import com.kumbukaa.dto.LoanResponse;
import com.kumbukaa.dto.UserDetailDto;
import com.kumbukaa.dto.UserSummaryDto;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.entity.User;
import com.kumbukaa.mapper.LoanResponseMapper;
import com.kumbukaa.repository.LoanBorrowedRepository;
import com.kumbukaa.repository.LoanLentRepository;
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
    private final LoanLentRepository loanLentRepository;
    private final LoanBorrowedRepository loanBorrowedRepository;

    public AdminService(UserRepository userRepository,
                        LoanLentRepository loanLentRepository,
                        LoanBorrowedRepository loanBorrowedRepository) {
        this.userRepository = userRepository;
        this.loanLentRepository = loanLentRepository;
        this.loanBorrowedRepository = loanBorrowedRepository;
    }

    public List<UserSummaryDto> listAllUsersWithLoans() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::toUserWithLoansDto).collect(Collectors.toList());
    }

    public UserDetailDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserDetailDto(user);
    }

    private UserSummaryDto toUserWithLoansDto(User user) {
        long lentCount = loanLentRepository.findAllByUserId(user.getId()).size();
        long borrowedCount = loanBorrowedRepository.findAllByUserId(user.getId()).size();

        return UserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .loansLent(lentCount)
                .loansBorrowed(borrowedCount)
                .build();
    }

    private UserDetailDto toUserDetailDto(User user) {
        List<LoanLent> loansLent = loanLentRepository.findAllByUserId(user.getId());
        List<LoanBorrowed> loansBorrowed = loanBorrowedRepository.findAllByUserId(user.getId());

        java.util.List<LoanResponse> lentResponses = loansLent.stream()
                .map(LoanResponseMapper::toResponse)
                .collect(Collectors.toList());
        java.util.List<LoanResponse> borrowedResponses = loansBorrowed.stream()
                .map(LoanResponseMapper::toResponse)
                .collect(Collectors.toList());

        return UserDetailDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .loansLent(lentResponses)
                .loansBorrowed(borrowedResponses)
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
