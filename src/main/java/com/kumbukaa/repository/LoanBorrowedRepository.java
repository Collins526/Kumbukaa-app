package com.kumbukaa.repository;

import com.kumbukaa.entity.LoanBorrowed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanBorrowedRepository extends JpaRepository<LoanBorrowed, Long> {
    List<LoanBorrowed> findAllByUserId(Long userId);
    Optional<LoanBorrowed> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT l FROM LoanBorrowed l WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(l.phoneNumber, ' ', ''), '-', ''), '+', ''), '.', ''), '(', ''), ')', '') = :phoneNumber")
    List<LoanBorrowed> findAllByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    boolean existsByUserIdAndPhoneNumberAndAmountBorrowedAndDateBorrowed(Long userId, String phoneNumber, Double amountBorrowed, java.time.LocalDate dateBorrowed);
}
