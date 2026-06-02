package com.kumbukaa.repository;

import com.kumbukaa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findFirstByPhoneNumber(String phoneNumber);
    List<User> findAllByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
}
