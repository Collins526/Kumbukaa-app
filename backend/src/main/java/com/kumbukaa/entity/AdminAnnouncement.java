package com.kumbukaa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_announcement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAnnouncement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    private String audience;
    private String channel;
    private Long targetUserId;
    private String targetName;
    private Long authorId;
    private String authorName;
    private Integer recipientCount;
    private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
