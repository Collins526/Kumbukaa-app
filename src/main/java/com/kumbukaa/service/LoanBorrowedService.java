package com.kumbukaa.service;

import com.kumbukaa.dto.LoanBorrowedRequest;
import com.kumbukaa.dto.PaymentRequest;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanPayment;
import com.kumbukaa.enums.PersonalLoanStatus;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.LoanBorrowedRepository;
import com.kumbukaa.repository.LoanLentRepository;
import com.kumbukaa.repository.UserRepository;
import com.kumbukaa.util.PhoneNumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@SuppressWarnings("null")
public class LoanBorrowedService {

    private final LoanBorrowedRepository repository;
    private final LoanLentRepository lentRepository;
    private final UserRepository userRepository;

    public LoanBorrowedService(LoanBorrowedRepository repository, LoanLentRepository lentRepository, UserRepository userRepository) {
        this.repository = repository;
        this.lentRepository = lentRepository;
        this.userRepository = userRepository;
    }

    public LoanBorrowed createLoan(LoanBorrowedRequest request, Long userId) {
        validateLoanRequest(request.getPersonName(), request.getPhoneNumber(), request.getAmountBorrowed());

        LoanBorrowed loan = LoanBorrowed.builder()
                .userId(userId)
                .personName(request.getPersonName())
                .phoneNumber(PhoneNumberUtils.normalize(request.getPhoneNumber()))
                .amountBorrowed(request.getAmountBorrowed())
                .amountPaid(0.0)
                .balance(request.getAmountBorrowed())
                .dateBorrowed(request.getDateBorrowed())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .status(computeStatus(request.getAmountBorrowed(), 0.0, request.getDueDate()))
                .build();

        LoanBorrowed savedLoan = repository.save(loan);
        syncLentMirror(savedLoan);
        return savedLoan;
    }

    public List<LoanBorrowed> findAll(Long userId) {
        return repository.findAllByUserId(userId);
    }

    public Optional<LoanBorrowed> findById(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId);
    }

    public LoanBorrowed updateLoan(Long id, LoanBorrowedRequest request, Long userId) {
        LoanBorrowed loan = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("LoanBorrowed not found for id: " + id));

        validateLoanRequest(request.getPersonName(), request.getPhoneNumber(), request.getAmountBorrowed());

        loan.setPersonName(request.getPersonName());
        loan.setPhoneNumber(PhoneNumberUtils.normalize(request.getPhoneNumber()));

        if (request.getAmountBorrowed() != null && request.getAmountBorrowed() > 0) {
            loan.setAmountBorrowed(request.getAmountBorrowed());
            loan.setBalance(Math.max(0.0, request.getAmountBorrowed() - loan.getAmountPaid()));
        }

        loan.setDateBorrowed(request.getDateBorrowed());
        loan.setDueDate(request.getDueDate());
        loan.setNotes(request.getNotes());
        loan.setStatus(computeStatus(loan.getBalance(), loan.getAmountPaid(), loan.getDueDate()));

        LoanBorrowed savedLoan = repository.save(loan);
        syncLentMirror(savedLoan);
        return savedLoan;
    }

    public void deleteLoan(Long id, Long userId) {
        LoanBorrowed loan = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("LoanBorrowed not found for id: " + id));
        repository.delete(loan);
    }

    public LoanBorrowed recordPayment(Long id, PaymentRequest request, Long userId) {
        if (request == null || request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        LoanBorrowed loan = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("LoanBorrowed not found for id: " + id));

        if (loan.getBalance() <= 0) {
            throw new IllegalStateException("Loan is already paid");
        }

        double newAmountPaid = loan.getAmountPaid() + request.getAmount();
        double newBalance = Math.max(0.0, loan.getAmountBorrowed() - newAmountPaid);

        LoanPayment payment = LoanPayment.builder()
                .amount(request.getAmount())
                .paymentDate(LocalDateTime.now())
                .loanBorrowed(loan)
                .build();
        loan.getPayments().add(payment);

        loan.setAmountPaid(newAmountPaid);
        loan.setBalance(newBalance);
        loan.setStatus(computeStatus(newBalance, newAmountPaid, loan.getDueDate()));

        LoanBorrowed savedLoan = repository.save(loan);
        syncLentMirror(savedLoan, payment);
        return savedLoan;
    }

    private void syncLentMirror(LoanBorrowed loan) {
        syncLentMirror(loan, null);
    }

    private void syncLentMirror(LoanBorrowed loan, LoanPayment payment) {
        if (loan.getUserId() == null) {
            return;
        }

        User currentUser = userRepository.findById(loan.getUserId()).orElse(null);
        if (currentUser == null) {
            return;
        }

        lentRepository.findByPersonNameAndPhoneNumberAndAmountLentAndDateLent(
                        currentUser.getFullName(),
                        currentUser.getPhoneNumber(),
                        loan.getAmountBorrowed(),
                        loan.getDateBorrowed())
                .ifPresent(mirror -> {
                    mirror.setAmountLent(loan.getAmountBorrowed());
                    mirror.setAmountPaid(loan.getAmountPaid());
                    mirror.setBalance(loan.getBalance());
                    mirror.setDateLent(loan.getDateBorrowed());
                    mirror.setDueDate(loan.getDueDate());
                    mirror.setStatus(loan.getStatus());
                    mirror.setNotes(loan.getNotes());
                    if (payment != null) {
                        mirror.getPayments().add(LoanPayment.builder()
                                .amount(payment.getAmount())
                                .paymentDate(payment.getPaymentDate())
                                .loanLent(mirror)
                                .build());
                    }
                    lentRepository.save(mirror);
                });
    }

    private PersonalLoanStatus computeStatus(Double balance, Double amountPaid, LocalDate dueDate) {
        if (balance <= 0) {
            return PersonalLoanStatus.PAID;
        }
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return PersonalLoanStatus.OVERDUE;
        }
        if (amountPaid != null && amountPaid > 0) {
            return PersonalLoanStatus.PARTIALLY_PAID;
        }
        return PersonalLoanStatus.ACTIVE;
    }

    private void validateLoanRequest(String personName, String phoneNumber, Double amount) {
        if (personName == null || personName.isBlank()) {
            throw new IllegalArgumentException("Person name is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount borrowed must be greater than zero");
        }
    }
}
