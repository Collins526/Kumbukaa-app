package com.kumbukaa.entity;

import com.kumbukaa.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User lender;

    @ManyToOne
    private User borrower;

    private Double amount;
    private Double balance;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;
}
