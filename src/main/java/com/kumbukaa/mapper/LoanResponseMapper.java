package com.kumbukaa.mapper;

import com.kumbukaa.dto.LoanResponse;
import com.kumbukaa.dto.PaymentSummary;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.entity.LoanPayment;
import com.kumbukaa.enums.PersonalLoanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class LoanResponseMapper {

    private LoanResponseMapper() {
    }

    public static LoanResponse toResponse(LoanLent loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanAmount(loan.getAmountLent())
                .amountPartiallyPaid(resolveAmountPartiallyPaid(loan.getAmountPaid(), loan.getBalance()))
                .balance(loan.getBalance())
                .personName(loan.getPersonName())
                .phoneNumber(loan.getPhoneNumber())
                .dueDate(resolveDueDate(loan.getStatus(), loan.getDueDate()))
                .paymentDate(resolvePaymentDate(loan.getStatus(), loan.getPayments()))
                .status(resolveStatus(loan.getStatus()))
                .installments(resolveInstallments(loan.getPayments()))
                .build();
    }

    public static LoanResponse toResponse(LoanBorrowed loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanAmount(loan.getAmountBorrowed())
                .amountPartiallyPaid(resolveAmountPartiallyPaid(loan.getAmountPaid(), loan.getBalance()))
                .balance(loan.getBalance())
                .personName(loan.getPersonName())
                .phoneNumber(loan.getPhoneNumber())
                .dueDate(resolveDueDate(loan.getStatus(), loan.getDueDate()))
                .paymentDate(resolvePaymentDate(loan.getStatus(), loan.getPayments()))
                .status(resolveStatus(loan.getStatus()))
                .installments(resolveInstallments(loan.getPayments()))
                .build();
    }

    private static LocalDate resolveDueDate(PersonalLoanStatus status, LocalDate dueDate) {
        return status == PersonalLoanStatus.PAID ? null : dueDate;
    }

    private static List<PaymentSummary> resolveInstallments(List<LoanPayment> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        return payments.stream()
                .filter(payment -> payment != null)
                .map(payment -> PaymentSummary.builder()
                        .amount(payment.getAmount())
                        .paymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate().toLocalDate() : null)
                        .build())
                .toList();
    }

    private static Double resolveAmountPartiallyPaid(Double amountPaid, Double balance) {
        if (amountPaid == null || amountPaid <= 0) {
            return 0.0;
        }
        return balance == null || balance <= 0 ? amountPaid : amountPaid;
    }

    private static LocalDate resolvePaymentDate(PersonalLoanStatus status, List<LoanPayment> payments) {
        if (status != PersonalLoanStatus.PAID) {
            return null;
        }
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        return payments.stream()
                .map(LoanPayment::getPaymentDate)
                .filter(date -> date != null)
                .max(Comparator.naturalOrder())
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
    }

    private static String resolveStatus(PersonalLoanStatus status) {
        return status == null ? null : status.name();
    }
}
