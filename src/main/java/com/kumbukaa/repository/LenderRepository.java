package com.kumbukaa.repository;

import com.kumbukaa.entity.Lender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LenderRepository extends JpaRepository<Lender, Long> {
    Optional<Lender> findByUserId(Long userId);
}
