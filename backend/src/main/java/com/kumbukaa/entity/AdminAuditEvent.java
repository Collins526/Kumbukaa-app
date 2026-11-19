package com.kumbukaa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String action;
    private String targetType;
    private Long actorId;
    private String actorName;
    private Long targetId;
    private String targetName;
    @Column(columnDefinition = "TEXT")
    private String details;
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
