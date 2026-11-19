package com.kumbukaa.dto;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminLoanDto {
    private Long id;
    private String kind;
    private Long accountId;
    private String accountName, accountEmail, counterpartyName, counterpartyPhone, counterpartyEmail;
    private Double amount, amountPaid, balance;
    private LocalDate startedAt, dueDate;
    private String status;
    private List<PaymentSummary> installments;
}
