package com.kumbukaa.service;

import com.kumbukaa.dto.PaymentRequest;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.entity.User;
import com.kumbukaa.enums.PersonalLoanStatus;
import com.kumbukaa.repository.LoanBorrowedRepository;
import com.kumbukaa.repository.LoanLentRepository;
import com.kumbukaa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class LoanPaymentMirrorSyncTest {

    @Test
    void recordsPaymentOnLentLoanAndUpdatesBorrowedMirror() {
        LoanLentRepository lentRepository = mock(LoanLentRepository.class);
        LoanBorrowedRepository borrowedRepository = mock(LoanBorrowedRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        LoanLentService service = new LoanLentService(lentRepository, borrowedRepository, userRepository);

        LoanLent loan = LoanLent.builder()
                .id(1L)
                .userId(10L)
                .personName("Borrower")
                .phoneNumber("+254700000000")
                .amountLent(5000.0)
                .amountPaid(1000.0)
                .balance(4000.0)
                .dateLent(LocalDate.of(2026, 6, 1))
                .dueDate(LocalDate.of(2026, 7, 1))
                .status(PersonalLoanStatus.PARTIALLY_PAID)
                .build();

        LoanBorrowed mirror = LoanBorrowed.builder()
                .id(2L)
                .personName("Lender")
                .phoneNumber("+254799999999")
                .amountBorrowed(5000.0)
                .amountPaid(1000.0)
                .balance(4000.0)
                .dateBorrowed(LocalDate.of(2026, 6, 1))
                .dueDate(LocalDate.of(2026, 7, 1))
                .status(PersonalLoanStatus.PARTIALLY_PAID)
                .build();

        when(lentRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(loan));
        when(lentRepository.save(loan)).thenReturn(loan);
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().fullName("Lender").phoneNumber("+254799999999").build()));
        when(borrowedRepository.findByPersonNameAndPhoneNumberAndAmountBorrowedAndDateBorrowed("Lender", "+254799999999", 5000.0, LocalDate.of(2026, 6, 1)))
                .thenReturn(Optional.of(mirror));

        service.recordPayment(1L, new PaymentRequest(500.0), 10L);

        ArgumentCaptor<LoanBorrowed> captor = ArgumentCaptor.forClass(LoanBorrowed.class);
        verify(borrowedRepository).save(captor.capture());

        assertEquals(1500.0, captor.getValue().getAmountPaid());
        assertEquals(3500.0, captor.getValue().getBalance());
        assertEquals(PersonalLoanStatus.PARTIALLY_PAID, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getPayments().size());
        assertEquals(500.0, captor.getValue().getPayments().get(0).getAmount());
    }
}
