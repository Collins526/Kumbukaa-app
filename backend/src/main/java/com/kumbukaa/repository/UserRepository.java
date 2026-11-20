package com.kumbukaa.repository;

import com.kumbukaa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(u.phoneNumber, ' ', ''), '-', ''), '+', ''), '.', ''), '(', ''), ')', '') = :phoneNumber")
    List<User> findAllByNormalizedPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
