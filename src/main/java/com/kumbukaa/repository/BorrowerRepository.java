package com.kumbukaa.repository;

import com.kumbukaa.entity.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, Long> {
    Optional<Borrower> findByUserId(Long userId);
    Optional<Borrower> findByIdNumber(String idNumber);
}
