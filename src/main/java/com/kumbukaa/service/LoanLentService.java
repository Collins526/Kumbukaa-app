package com.kumbukaa.service;

import com.kumbukaa.dto.LoanLentRequest;
import com.kumbukaa.dto.PaymentRequest;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.enums.PersonalLoanStatus;
import com.kumbukaa.repository.LoanLentRepository;
import com.kumbukaa.util.PhoneNumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@SuppressWarnings("null")
public class LoanLentService {

    private final LoanLentRepository repository;

    public LoanLentService(LoanLentRepository repository) {
        this.repository = repository;
    }

    public LoanLent createLoan(LoanLentRequest request, Long userId) {
        validateLoanRequest(request.getPersonName(), request.getPhoneNumber(), request.getAmountLent());

        LoanLent loan = LoanLent.builder()
                .userId(userId)
                .personName(request.getPersonName())
                .phoneNumber(PhoneNumberUtils.normalize(request.getPhoneNumber()))
                .amountLent(request.getAmountLent())
                .amountPaid(0.0)
                .balance(request.getAmountLent())
                .dateLent(request.getDateLent())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .status(computeStatus(request.getAmountLent(), 0.0, request.getDueDate()))
                .build();

        return repository.save(loan);
    }

    public List<LoanLent> findAll(Long userId) {
        return repository.findAllByUserId(userId);
    }

    public Optional<LoanLent> findById(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId);
    }

    public LoanLent updateLoan(Long id, LoanLentRequest request, Long userId) {
        LoanLent loan = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("LoanLent not found for id: " + id));

        validateLoanRequest(request.getPersonName(), request.getPhoneNumber(), request.getAmountLent());

        loan.setPersonName(request.getPersonName());
        loan.setPhoneNumber(PhoneNumberUtils.normalize(request.getPhoneNumber()));

        if (request.getAmountLent() != null && request.getAmountLent() > 0) {
            loan.setAmountLent(request.getAmountLent());
            loan.setBalance(Math.max(0.0, request.getAmountLent() - loan.getAmountPaid()));
        }

        loan.setDateLent(request.getDateLent());
        loan.setDueDate(request.getDueDate());
        loan.setNotes(request.getNotes());
        loan.setStatus(computeStatus(loan.getBalance(), loan.getAmountPaid(), loan.getDueDate()));

        return repository.save(loan);
    }

    public void deleteLoan(Long id, Long userId) {
        LoanLent loan = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("LoanLent not found for id: " + id));
        repository.delete(loan);
    }

    public LoanLent recordPayment(Long id, PaymentRequest request, Long userId) {
        if (request == null || request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        LoanLent loan = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("LoanLent not found for id: " + id));

        if (loan.getBalance() <= 0) {
            throw new IllegalStateException("Loan is already paid");
        }

        double newAmountPaid = loan.getAmountPaid() + request.getAmount();
        double newBalance = Math.max(0.0, loan.getAmountLent() - newAmountPaid);

        loan.setAmountPaid(newAmountPaid);
        loan.setBalance(newBalance);
        loan.setStatus(computeStatus(newBalance, newAmountPaid, loan.getDueDate()));

        return repository.save(loan);
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
            throw new IllegalArgumentException("Amount lent must be greater than zero");
        }
    }
}
