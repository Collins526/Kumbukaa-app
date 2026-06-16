package com.kumbukaa.mapper;

import com.kumbukaa.dto.LoanResponse;
import com.kumbukaa.dto.PaymentSummary;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.entity.LoanPayment;
import com.kumbukaa.enums.PersonalLoanStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

public final class LoanResponseMapper {

    private LoanResponseMapper() {
    }

    public static LoanResponse toResponse(LoanLent loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanAmount(loan.getAmountLent())
                .amountPaid(resolveAmountPaid(loan.getAmountPaid()))
                .balance(loan.getBalance())
                .personName(loan.getPersonName())
                .phoneNumber(loan.getPhoneNumber())
                .dateLent(resolveDate(loan.getDateLent()))
                .dueDate(resolveDueDate(loan.getStatus(), loan.getDueDate()))
                .paymentDate(resolvePaymentDate(loan.getStatus(), loan.getPayments()))
                .status(resolveStatus(loan.getStatus()))
                .notes(loan.getNotes())
                .installments(resolveInstallments(loan.getPayments()))
                .build();
    }

    public static LoanResponse toResponse(LoanBorrowed loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanAmount(loan.getAmountBorrowed())
                .amountPaid(resolveAmountPaid(loan.getAmountPaid()))
                .balance(loan.getBalance())
                .personName(loan.getPersonName())
                .phoneNumber(loan.getPhoneNumber())
                .dateBorrowed(resolveDate(loan.getDateBorrowed()))
                .dueDate(resolveDueDate(loan.getStatus(), loan.getDueDate()))
                .paymentDate(resolvePaymentDate(loan.getStatus(), loan.getPayments()))
                .status(resolveStatus(loan.getStatus()))
                .notes(loan.getNotes())
                .installments(resolveInstallments(loan.getPayments()))
                .build();
    }

    private static OffsetDateTime resolveDueDate(PersonalLoanStatus status, LocalDate dueDate) {
        if (dueDate == null) {
            return null;
        }
        return dueDate.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private static List<PaymentSummary> resolveInstallments(List<LoanPayment> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        return payments.stream()
                .filter(payment -> payment != null)
                .sorted(Comparator.comparing(LoanPayment::getPaymentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(payment -> PaymentSummary.builder()
                        .amount(payment.getAmount())
                        .paymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate().atOffset(ZoneOffset.UTC) : null)
                        .build())
                .toList();
    }

    private static Double resolveAmountPaid(Double amountPaid) {
        if (amountPaid == null || amountPaid <= 0) {
            return 0.0;
        }
        return amountPaid;
    }

    private static OffsetDateTime resolvePaymentDate(PersonalLoanStatus status, List<LoanPayment> payments) {
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
                .map(date -> date.atOffset(ZoneOffset.UTC))
                .orElse(null);
    }

    private static OffsetDateTime resolveDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private static String resolveStatus(PersonalLoanStatus status) {
        return status == null ? null : status.name();
    }
}
