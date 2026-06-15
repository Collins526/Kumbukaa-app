package com.kumbukaa.repository;

import com.kumbukaa.entity.LoanLent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LoanLentRepository extends JpaRepository<LoanLent, Long> {
    List<LoanLent> findAllByUserId(Long userId);
    Optional<LoanLent> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT l FROM LoanLent l WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(l.phoneNumber, ' ', ''), '-', ''), '+', ''), '.', ''), '(', ''), ')', '') = :phoneNumber")
    List<LoanLent> findAllByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    Optional<LoanLent> findByPersonNameAndPhoneNumberAndAmountLentAndDateLent(String personName, String phoneNumber, Double amountLent, java.time.LocalDate dateLent);

    boolean existsByUserIdAndPhoneNumberAndAmountLentAndDateLent(Long userId, String phoneNumber, Double amountLent, java.time.LocalDate dateLent);
}
