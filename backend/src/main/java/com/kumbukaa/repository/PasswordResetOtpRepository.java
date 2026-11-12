package com.kumbukaa.repository;

import com.kumbukaa.entity.PasswordResetOtp;
import com.kumbukaa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    List<PasswordResetOtp> findAllByUserAndUsedFalseAndVerifiedFalseOrderByCreatedAtDesc(User user);
    List<PasswordResetOtp> findAllByUserAndUsedFalse(User user);
    Optional<PasswordResetOtp> findByUserAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(User user);
}
