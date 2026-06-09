package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    private Long id;
    private Double loanAmount;
    private Double amountPartiallyPaid;
    private Double balance;
    private String personName;
    private String phoneNumber;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private String status;
    private List<PaymentSummary> installments;
}
