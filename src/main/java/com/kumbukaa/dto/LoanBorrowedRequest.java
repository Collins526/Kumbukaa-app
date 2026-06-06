package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanBorrowedRequest {
    private String personName;
    private String phoneNumber;
    private Double amountBorrowed;
    private LocalDate dateBorrowed;
    private LocalDate dueDate;
    private String notes;
}
