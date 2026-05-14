package com.kumbukaa.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "borrower")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Borrower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "employment_status")
    private String employmentStatus;

    @Column(name = "annual_income")
    private Double annualIncome;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "total_borrowed")
    private Double totalBorrowed = 0.0;

    @Column(name = "total_repaid")
    private Double totalRepaid = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
