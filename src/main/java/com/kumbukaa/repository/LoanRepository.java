package com.kumbukaa.repository;

import com.kumbukaa.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByLenderId(Long lenderId);
    List<Loan> findByBorrowerId(Long borrowerId);
}
