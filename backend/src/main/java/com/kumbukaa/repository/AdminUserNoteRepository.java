package com.kumbukaa.repository;
import com.kumbukaa.entity.AdminUserNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AdminUserNoteRepository extends JpaRepository<AdminUserNote, Long> {
    List<AdminUserNote> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
