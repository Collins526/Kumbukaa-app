package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDto {
    private String payerPhone;
    private Long loanId;
    private Double amount;
    private String mpesaCode;
}
