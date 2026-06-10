package com.kumbukaa.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.kumbukaa.enums.PersonalLoanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_borrowed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanBorrowed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String personName;
    private String phoneNumber;
    private Double amountBorrowed;
    private Double amountPaid;
    private Double balance;
    private LocalDate dateBorrowed;
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private PersonalLoanStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "loanBorrowed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference(value = "loanBorrowed-payments")
    @Builder.Default
    private List<LoanPayment> payments = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
