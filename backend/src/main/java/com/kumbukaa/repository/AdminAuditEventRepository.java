package com.kumbukaa.repository;
import com.kumbukaa.entity.AdminAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AdminAuditEventRepository extends JpaRepository<AdminAuditEvent, Long> {}
