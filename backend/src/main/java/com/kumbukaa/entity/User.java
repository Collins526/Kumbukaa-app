package com.kumbukaa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phoneNumber;

    @Column
    private String passwordHash;

    /** Comma-separated roles, e.g. "ROLE_USER,ROLE_ADMIN" */
    private String roles;

    private Boolean mustChangePassword;

    @Builder.Default
    private String accountStatus = "ACTIVE";

    @Builder.Default
    private String verificationStatus = "PENDING";

    private LocalDateTime lastSeenAt;
    private String lastDevice;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (accountStatus == null) accountStatus = "ACTIVE";
        if (verificationStatus == null) verificationStatus = "PENDING";
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        if (accountStatus == null) accountStatus = "ACTIVE";
        if (verificationStatus == null) verificationStatus = "PENDING";
        updatedAt = LocalDateTime.now();
    }
}
