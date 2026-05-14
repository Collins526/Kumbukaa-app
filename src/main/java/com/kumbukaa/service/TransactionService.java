package com.kumbukaa.service;

import com.kumbukaa.entity.Loan;
import com.kumbukaa.entity.Transaction;
import com.kumbukaa.enums.LoanStatus;
import com.kumbukaa.enums.TransactionStatus;
import com.kumbukaa.repository.LoanRepository;
import com.kumbukaa.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository txRepo;
    private final LoanRepository loanRepo;

    public TransactionService(TransactionRepository txRepo, LoanRepository loanRepo) {
        this.txRepo = txRepo;
        this.loanRepo = loanRepo;
    }

    public Transaction logPayment(Long loanId, Double amount, String mpesaCode) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        Loan loan = loanRepo.findById(loanId).orElseThrow();

        Transaction tx = new Transaction();
        tx.setLoan(loan);
        tx.setAmount(amount);
        tx.setMpesaTransactionId(mpesaCode);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setCreatedAt(LocalDateTime.now());

        return txRepo.save(tx);
    }

    public Transaction approveTransaction(Long txId) {
        if (txId == null) {
            throw new IllegalArgumentException("Transaction ID must not be null");
        }
        Transaction tx = txRepo.findById(txId).orElseThrow();
        Loan loan = tx.getLoan();

        tx.setStatus(TransactionStatus.APPROVED);
        loan.setBalance(loan.getBalance() - tx.getAmount());

        if (loan.getBalance() <= 0) {
            loan.setStatus(LoanStatus.SETTLED);
        }

        loanRepo.save(loan);
        return txRepo.save(tx);
    }

    public Optional<Transaction> findById(Long id) {
        if (id == null) return Optional.empty();
        return txRepo.findById(id);
    }

    public List<Transaction> findAll() {
        return txRepo.findAll();
    }

    public List<Transaction> findByLoanId(Long loanId) {
        if (loanId == null) return List.of();
        return txRepo.findByLoanId(loanId);
    }

    public Optional<Transaction> findByMpesaTransactionId(String mpesaCode) {
        if (mpesaCode == null) return Optional.empty();
        return txRepo.findByMpesaTransactionId(mpesaCode);
    }

    public Transaction updateTransaction(Transaction tx) {
        if (tx == null || tx.getId() == null) {
            throw new IllegalArgumentException("Transaction and Transaction ID must not be null");
        }
        return txRepo.save(tx);
    }

    public void deleteTransaction(Long id) {
        if (id != null) {
            txRepo.deleteById(id);
        }
    }
}
