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

    public LoanService(LoanRepository loanRepo, UserRepository userRepo) {
        this.loanRepo = loanRepo;
        this.userRepo = userRepo;
    }

    public Loan createLoan(Long lenderId, Long borrowerId, Double amount) {
        if (lenderId == null || borrowerId == null) {
            throw new IllegalArgumentException("Lender ID and Borrower ID must not be null");
        }
        User lender = userRepo.findById(lenderId).orElseThrow();
        User borrower = userRepo.findById(borrowerId).orElseThrow();

        Loan loan = new Loan();
        loan.setLender(lender);
        loan.setBorrower(borrower);
        loan.setAmount(amount);
        loan.setBalance(amount);
        loan.setStatus(LoanStatus.CREATED);

        return loanRepo.save(loan);
    }

    public Loan acceptLoan(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepo.save(loan);
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
