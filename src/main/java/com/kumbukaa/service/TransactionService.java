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
    private final NotificationService notificationService;

    public TransactionService(TransactionRepository txRepo, LoanRepository loanRepo, NotificationService notificationService) {
        this.txRepo = txRepo;
        this.loanRepo = loanRepo;
        this.notificationService = notificationService;
    }

    public Transaction markMoneyAsSent(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Money can only be marked as sent for active loans");
        }

        // Find or create transaction for this loan
        List<Transaction> transactions = txRepo.findByLoanId(loanId);
        Transaction tx = transactions.isEmpty() ? new Transaction() : transactions.get(0);
        
        tx.setLoan(loan);
        tx.setAmount(loan.getAmount());
        tx.setStatus(TransactionStatus.UNPAID);
        tx.setCreatedAt(LocalDateTime.now());

        Transaction savedTx = txRepo.save(tx);
        notificationService.sendNotification(loan.getBorrower(),
                String.format("Lender has sent Ksh %.2f for loan %d. Payment is now outstanding.", loan.getAmount(), loan.getId()));
        return savedTx;
    }

    public Transaction logPayment(Long loanId, Double amount, String mpesaCode) {
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID must not be null");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Payments can only be logged for active loans");
        }

        // Find the main transaction for this loan
        List<Transaction> transactions = txRepo.findByLoanId(loanId);
        if (transactions.isEmpty()) {
            throw new IllegalStateException("No transaction found for this loan. Lender must send money first.");
        }

        Transaction tx = transactions.get(0);
        if (tx.getStatus() != TransactionStatus.UNPAID) {
            throw new IllegalStateException("Payments can only be logged for unpaid transactions");
        }

        tx.setMpesaTransactionId(mpesaCode);
        // Store the payment amount separately or update based on approval
        Transaction savedTx = txRepo.save(tx);
        notificationService.sendNotification(loan.getLender(),
                String.format("Borrower submitted payment of Ksh %.2f (M-Pesa: %s) for loan %d. Please verify.", amount, mpesaCode, loan.getId()));
        return savedTx;
    }

    public Transaction approvePayment(Long txId, Double paymentAmount) {
        if (txId == null) {
            throw new IllegalArgumentException("Transaction ID must not be null");
        }
        if (paymentAmount == null || paymentAmount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        Transaction tx = txRepo.findById(txId).orElseThrow();
        if (tx.getStatus() != TransactionStatus.UNPAID) {
            throw new IllegalStateException("Only unpaid transactions can receive payments");
        }

        Loan loan = tx.getLoan();
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Only active loans can receive payments");
        }

        loan.setBalance(Math.max(0.0, loan.getBalance() - paymentAmount));

        if (loan.getBalance() <= 0) {
            loan.setStatus(LoanStatus.SETTLED);
            tx.setStatus(TransactionStatus.PAID);
            notificationService.sendNotification(loan.getBorrower(),
                    String.format("Your loan of Ksh %.2f has been fully paid. Thank you!", tx.getAmount()));
        } else {
            // Still unpaid but amount reduced
            notificationService.sendNotification(loan.getBorrower(),
                    String.format("Payment of Ksh %.2f approved. Remaining balance: Ksh %.2f.",
                            paymentAmount, loan.getBalance()));
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
