package com.kumbukaa.repository;
import com.kumbukaa.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<AppNotification> findByIdAndUserId(Long id, Long userId);
}
