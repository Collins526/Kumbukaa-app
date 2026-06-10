package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    private Long id;
    private Double loanAmount;
    private Double amountPaid;
    private Double balance;
    private String personName;
    private String phoneNumber;
    private OffsetDateTime dueDate;
    private OffsetDateTime paymentDate;
    private String status;
    private String notes;
    private List<PaymentSummary> installments;
}
