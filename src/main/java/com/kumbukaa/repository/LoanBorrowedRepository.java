package com.kumbukaa.repository;

import com.kumbukaa.entity.LoanBorrowed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanBorrowedRepository extends JpaRepository<LoanBorrowed, Long> {
}
