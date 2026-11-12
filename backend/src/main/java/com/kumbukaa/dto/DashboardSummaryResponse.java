package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private Double totalLent;
    private Double totalBorrowed;
    private Double amountOwedToMe;
    private Double amountIOwe;
    private Long activeLoansLent;
    private Long activeLoansBorrowed;
    private Long overdueLoans;
}
