package com.kumbukaa.repository;

import com.kumbukaa.entity.LoanBorrowed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanBorrowedRepository extends JpaRepository<LoanBorrowed, Long> {
    List<LoanBorrowed> findAllByUserId(Long userId);
    Optional<LoanBorrowed> findByIdAndUserId(Long id, Long userId);
}
