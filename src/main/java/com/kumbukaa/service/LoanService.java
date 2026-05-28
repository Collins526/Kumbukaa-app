package com.kumbukaa.service;

import com.kumbukaa.entity.Loan;
import com.kumbukaa.entity.User;
import com.kumbukaa.enums.LoanStatus;
import com.kumbukaa.repository.LoanRepository;
import com.kumbukaa.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;


@Service
public class LoanService {

    private final LoanRepository loanRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    public LoanService(LoanRepository loanRepo, UserRepository userRepo, NotificationService notificationService) {
        this.loanRepo = loanRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
    }

    public Loan createLoan(Long lenderId, Long borrowerId, Double amount) {
        if (lenderId == null || borrowerId == null) {
            throw new IllegalArgumentException("Lender ID and Borrower ID must not be null");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero");
        }
        if (lenderId.equals(borrowerId)) {
            throw new IllegalArgumentException("Lender and borrower must be different users");
        }

        User lender = userRepo.findById(lenderId).orElseThrow();
        User borrower = userRepo.findById(borrowerId).orElseThrow();

        Loan loan = new Loan();
        loan.setLender(lender);
        loan.setBorrower(borrower);
        loan.setAmount(amount);
        loan.setBalance(amount);
        loan.setStatus(LoanStatus.PENDING);

        Loan savedLoan = loanRepo.save(loan);
        notificationService.sendNotification(lender,
                String.format("New loan request from %s for Ksh %.2f. Review and respond.", borrower.getName(), amount));
        return savedLoan;
    }

    public Loan acceptLoan(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be accepted");
        }

        loan.setStatus(LoanStatus.ACTIVE);
        Loan savedLoan = loanRepo.save(loan);
        notificationService.sendNotification(loan.getBorrower(),
                String.format("Your loan request of Ksh %.2f has been approved by %s.", loan.getAmount(), loan.getLender().getName()));
        notificationService.sendNotification(loan.getLender(),
                String.format("Proceed to send Ksh %.2f to %s for loan %d.", loan.getAmount(), loan.getBorrower().getName(), loan.getId()));
        return savedLoan;
    }

    public Loan rejectLoan(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be rejected");
        }

        loan.setStatus(LoanStatus.REJECTED);
        Loan savedLoan = loanRepo.save(loan);
        notificationService.sendNotification(loan.getBorrower(),
                String.format("Your loan request of Ksh %.2f was rejected by %s.", loan.getAmount(), loan.getLender().getName()));
        return savedLoan;
    }

    public Optional<Loan> findById(Long id) {
        if (id == null) return Optional.empty();
        return loanRepo.findById(id);
    }

    public List<Loan> findAll() {
        return loanRepo.findAll();
    }

    public List<Loan> findByLenderId(Long lenderId) {
        if (lenderId == null) return List.of();
        return loanRepo.findByLenderId(lenderId);
    }

    public List<Loan> findByBorrowerId(Long borrowerId) {
        if (borrowerId == null) return List.of();
        return loanRepo.findByBorrowerId(borrowerId);
    }

    public Loan updateLoan(Loan loan) {
        if (loan == null || loan.getId() == null) {
            throw new IllegalArgumentException("Loan and Loan ID must not be null");
        }
        return loanRepo.save(loan);
    }

    public void deleteLoan(Long id) {
        if (id != null) {
            loanRepo.deleteById(id);
        }
    }
}
