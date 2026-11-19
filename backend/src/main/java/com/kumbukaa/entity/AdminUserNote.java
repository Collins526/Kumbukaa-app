package com.kumbukaa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_user_note")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
