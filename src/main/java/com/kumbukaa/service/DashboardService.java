package com.kumbukaa.service;

import com.kumbukaa.dto.DashboardSummaryResponse;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.enums.PersonalLoanStatus;
import com.kumbukaa.repository.LoanBorrowedRepository;
import com.kumbukaa.repository.LoanLentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final LoanLentRepository lentRepository;
    private final LoanBorrowedRepository borrowedRepository;

    public DashboardService(LoanLentRepository lentRepository, LoanBorrowedRepository borrowedRepository) {
        this.lentRepository = lentRepository;
        this.borrowedRepository = borrowedRepository;
    }

    public DashboardSummaryResponse getSummary() {
        List<LoanLent> lentLoans = lentRepository.findAll();
        List<LoanBorrowed> borrowedLoans = borrowedRepository.findAll();

        double totalLent = lentLoans.stream()
                .mapToDouble(loan -> loan.getAmountLent() != null ? loan.getAmountLent() : 0.0)
                .sum();
        double totalBorrowed = borrowedLoans.stream()
                .mapToDouble(loan -> loan.getAmountBorrowed() != null ? loan.getAmountBorrowed() : 0.0)
                .sum();
        double amountOwedToMe = lentLoans.stream()
                .mapToDouble(loan -> loan.getBalance() != null ? loan.getBalance() : 0.0)
                .sum();
        double amountIOwe = borrowedLoans.stream()
                .mapToDouble(loan -> loan.getBalance() != null ? loan.getBalance() : 0.0)
                .sum();
        long activeLoansLent = lentLoans.stream()
                .filter(loan -> loan.getStatus() == PersonalLoanStatus.ACTIVE || loan.getStatus() == PersonalLoanStatus.PARTIALLY_PAID)
                .count();
        long activeLoansBorrowed = borrowedLoans.stream()
                .filter(loan -> loan.getStatus() == PersonalLoanStatus.ACTIVE || loan.getStatus() == PersonalLoanStatus.PARTIALLY_PAID)
                .count();
        long overdueLoans = lentLoans.stream()
                .filter(loan -> loan.getStatus() == PersonalLoanStatus.OVERDUE)
                .count() + borrowedLoans.stream()
                .filter(loan -> loan.getStatus() == PersonalLoanStatus.OVERDUE)
                .count();

        return new DashboardSummaryResponse(
                totalLent,
                totalBorrowed,
                amountOwedToMe,
                amountIOwe,
                activeLoansLent,
                activeLoansBorrowed,
                overdueLoans
        );
    }
}
