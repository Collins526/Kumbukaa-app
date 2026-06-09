package com.kumbukaa.mapper;

import com.kumbukaa.dto.LoanResponse;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.entity.LoanPayment;
import com.kumbukaa.enums.PersonalLoanStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoanResponseMapperTest {

    @Test
    void mapsLentLoanToResponseWithDueDateWhenOutstanding() {
        LoanLent loan = LoanLent.builder()
                .id(1L)
                .personName("John Doe")
                .phoneNumber("+254700000000")
                .amountLent(5000.0)
                .amountPaid(1000.0)
                .balance(4000.0)
                .dueDate(LocalDate.of(2026, 6, 20))
                .status(PersonalLoanStatus.ACTIVE)
                .build();

        LoanResponse response = LoanResponseMapper.toResponse(loan);

        assertEquals(1L, response.getId());
        assertEquals(5000.0, response.getLoanAmount());
        assertEquals(1000.0, response.getAmountPartiallyPaid());
        assertEquals(4000.0, response.getBalance());
        assertEquals("John Doe", response.getPersonName());
        assertEquals("+254700000000", response.getPhoneNumber());
        assertEquals(LocalDate.of(2026, 6, 20), response.getDueDate());
        assertNull(response.getPaymentDate());
    }

    @Test
    void mapsBorrowedLoanToResponseWithPaymentDateWhenPaid() {
        LoanBorrowed loan = LoanBorrowed.builder()
                .id(2L)
                .personName("Mary Smith")
                .phoneNumber("+254711111111")
                .amountBorrowed(3000.0)
                .amountPaid(3000.0)
                .balance(0.0)
                .dueDate(LocalDate.of(2026, 6, 10))
                .status(PersonalLoanStatus.PAID)
                .payments(List.of(
                        LoanPayment.builder()
                                .paymentDate(LocalDateTime.of(2026, 6, 5, 10, 15))
                                .build()
                ))
                .build();

        LoanResponse response = LoanResponseMapper.toResponse(loan);

        assertEquals(2L, response.getId());
        assertEquals(3000.0, response.getLoanAmount());
        assertEquals(3000.0, response.getAmountPartiallyPaid());
        assertEquals(0.0, response.getBalance());
        assertEquals("Mary Smith", response.getPersonName());
        assertEquals("+254711111111", response.getPhoneNumber());
        assertNull(response.getDueDate());
        assertEquals(LocalDate.of(2026, 6, 5), response.getPaymentDate());
    }
}
