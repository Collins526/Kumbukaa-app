package com.kumbukaa.repository;

import com.kumbukaa.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findFirstByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);
}
