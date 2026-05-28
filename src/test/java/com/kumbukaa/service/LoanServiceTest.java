package com.kumbukaa.service;

import com.kumbukaa.entity.Loan;
import com.kumbukaa.entity.User;
import com.kumbukaa.enums.LoanStatus;
import com.kumbukaa.repository.LoanRepository;
import com.kumbukaa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanService loanService;

    private User lender;
    private User borrower;
    private Loan loan;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test users
        lender = new User();
        lender.setId(1L);
        lender.setName("Alice Lender");
        lender.setEmail("alice@example.com");
        lender.setPhoneNumber("0700000001");
        lender.setPassword("hashedPassword");

        borrower = new User();
        borrower.setId(2L);
        borrower.setName("Bob Borrower");
        borrower.setEmail("bob@example.com");
        borrower.setPhoneNumber("0700000002");
        borrower.setPassword("hashedPassword");

        // Create test loan
        loan = new Loan();
        loan.setId(1L);
        loan.setLender(lender);
        loan.setBorrower(borrower);
        loan.setAmount(5000.0);
        loan.setBalance(5000.0);
        loan.setStatus(LoanStatus.PENDING);
    }

    @Test
    void testCreateLoan_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(lender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(borrower));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        // Act
        Loan createdLoan = loanService.createLoan(1L, 2L, 5000.0);

        // Assert
        assertNotNull(createdLoan);
        assertEquals(LoanStatus.PENDING, createdLoan.getStatus());
        assertEquals(5000.0, createdLoan.getAmount());
        assertEquals(5000.0, createdLoan.getBalance());
        assertEquals(lender, createdLoan.getLender());
        assertEquals(borrower, createdLoan.getBorrower());

        // Verify notification was sent to lender
        verify(notificationService, times(1)).sendNotification(
                eq(lender),
                anyString()
        );
    }

    @Test
    void testCreateLoan_InvalidAmount() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            loanService.createLoan(1L, 2L, 0.0);
        });
    }

    @Test
    void testCreateLoan_SameLenderAndBorrower() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            loanService.createLoan(1L, 1L, 5000.0);
        });
    }

    @Test
    void testCreateLoan_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(lender));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            loanService.createLoan(1L, 2L, 5000.0);
        });
    }

    @Test
    void testAcceptLoan_Success() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0, Loan.class);
            return savedLoan;
        });

        // Act
        Loan acceptedLoan = loanService.acceptLoan(1L);

        // Assert
        assertNotNull(acceptedLoan);
        assertEquals(LoanStatus.ACTIVE, acceptedLoan.getStatus());

        // Verify notifications were sent to both borrower and lender
        verify(notificationService, times(1)).sendNotification(
                eq(borrower),
                anyString()
        );
        verify(notificationService, times(1)).sendNotification(
                eq(lender),
                anyString()
        );
    }

    @Test
    void testAcceptLoan_NotPending() {
        // Arrange
        loan.setStatus(LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            loanService.acceptLoan(1L);
        });
    }

    @Test
    void testRejectLoan_Success() {
        // Arrange
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0, Loan.class);
            return savedLoan;
        });

        // Act
        Loan rejectedLoan = loanService.rejectLoan(1L);

        // Assert
        assertNotNull(rejectedLoan);
        assertEquals(LoanStatus.REJECTED, rejectedLoan.getStatus());

        // Verify notification was sent to borrower
        verify(notificationService, times(1)).sendNotification(
                eq(borrower),
                anyString()
        );
    }

    @Test
    void testRejectLoan_NotPending() {
        // Arrange
        loan.setStatus(LoanStatus.SETTLED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            loanService.rejectLoan(1L);
        });
    }
}
