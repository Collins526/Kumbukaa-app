package com.kumbukaa.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;
    private LocalDateTime paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_borrowed_id")
    @JsonBackReference(value = "loanBorrowed-payments")
    private LoanBorrowed loanBorrowed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_lent_id")
    @JsonBackReference(value = "loanLent-payments")
    private LoanLent loanLent;
}
