package com.kumbukaa.config;

import com.kumbukaa.entity.Loan;
import com.kumbukaa.enums.LoanStatus;
import com.kumbukaa.repository.LoanRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class LoanScheduler {

    private final LoanRepository repo;

    public LoanScheduler(LoanRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkOverdueLoans() {
        for (Loan loan : repo.findAll()) {
            if (loan.getDueDate() != null &&
                loan.getDueDate().isBefore(LocalDate.now()) &&
                loan.getStatus() == LoanStatus.ACTIVE) {

                loan.setStatus(LoanStatus.PAUSED);
                repo.save(loan);
            }
        }
    }
}
