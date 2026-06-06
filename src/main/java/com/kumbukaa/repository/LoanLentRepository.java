package com.kumbukaa.repository;

import com.kumbukaa.entity.LoanLent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanLentRepository extends JpaRepository<LoanLent, Long> {
}
