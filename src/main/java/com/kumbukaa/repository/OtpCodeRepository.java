package com.kumbukaa.repository;

import com.kumbukaa.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findFirstByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);
}
