package com.kumbukaa.service;

import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.LoanBorrowedRepository;
import com.kumbukaa.repository.LoanLentRepository;
import com.kumbukaa.repository.UserRepository;
import com.kumbukaa.util.PhoneNumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@SuppressWarnings("null")
public class LoanClaimService {

    private final LoanLentRepository loanLentRepository;
    private final LoanBorrowedRepository loanBorrowedRepository;
    private final UserRepository userRepository;

    public LoanClaimService(LoanLentRepository loanLentRepository,
                            LoanBorrowedRepository loanBorrowedRepository,
                            UserRepository userRepository) {
        this.loanLentRepository = loanLentRepository;
        this.loanBorrowedRepository = loanBorrowedRepository;
        this.userRepository = userRepository;
    }

    /**
     * Claims counterparty loan records for a newly registered user.
     *
     * When User A records "I lent to phone X", and person X registers,
     * a mirrored LoanBorrowed is created for the new user.
     *
     * When User A records "I borrowed from phone X", and person X registers,
     * a mirrored LoanLent is created for the new user.
     *
     * Original records are never modified or deleted.
     */
    public void claimCounterpartyLoans(User newUser) {
        String phone = PhoneNumberUtils.normalize(newUser.getPhoneNumber());
        if (phone == null || phone.isBlank()) {
            return;
        }

        claimFromLentRecords(newUser, phone);
        claimFromBorrowedRecords(newUser, phone);
    }

    /**
     * Someone recorded "I lent to <phone>" → the new user borrowed from them.
     * Creates mirrored LoanBorrowed records owned by the new user.
     */
    private void claimFromLentRecords(User newUser, String phone) {
        List<LoanLent> lentToNewUser = loanLentRepository.findAllByPhoneNumber(phone);

        for (LoanLent source : lentToNewUser) {
            if (source.getUserId() == null) {
                continue;
            }

            String lenderName = lookupUserFullName(source.getUserId());
            String lenderPhone = lookupUserPhone(source.getUserId());

            if (loanBorrowedRepository.existsByUserIdAndPhoneNumberAndAmountBorrowedAndDateBorrowed(
                    newUser.getId(), lenderPhone, source.getAmountLent(), source.getDateLent())) {
                continue;
            }

            LoanBorrowed mirror = LoanBorrowed.builder()
                    .userId(newUser.getId())
                    .personName(lenderName)
                    .phoneNumber(lenderPhone)
                    .amountBorrowed(source.getAmountLent())
                    .amountPaid(source.getAmountPaid())
                    .balance(source.getBalance())
                    .dateBorrowed(source.getDateLent())
                    .dueDate(source.getDueDate())
                    .notes(source.getNotes())
                    .status(source.getStatus())
                    .build();

            loanBorrowedRepository.save(mirror);
        }
    }

    /**
     * Someone recorded "I borrowed from <phone>" → the new user lent to them.
     * Creates mirrored LoanLent records owned by the new user.
     */
    private void claimFromBorrowedRecords(User newUser, String phone) {
        List<LoanBorrowed> borrowedFromNewUser = loanBorrowedRepository.findAllByPhoneNumber(phone);

        for (LoanBorrowed source : borrowedFromNewUser) {
            if (source.getUserId() == null) {
                continue;
            }

            String borrowerName = lookupUserFullName(source.getUserId());
            String borrowerPhone = lookupUserPhone(source.getUserId());

            if (loanLentRepository.existsByUserIdAndPhoneNumberAndAmountLentAndDateLent(
                    newUser.getId(), borrowerPhone, source.getAmountBorrowed(), source.getDateBorrowed())) {
                continue;
            }

            LoanLent mirror = LoanLent.builder()
                    .userId(newUser.getId())
                    .personName(borrowerName)
                    .phoneNumber(borrowerPhone)
                    .amountLent(source.getAmountBorrowed())
                    .amountPaid(source.getAmountPaid())
                    .balance(source.getBalance())
                    .dateLent(source.getDateBorrowed())
                    .dueDate(source.getDueDate())
                    .notes(source.getNotes())
                    .status(source.getStatus())
                    .build();

            loanLentRepository.save(mirror);
        }
    }

    private String lookupUserFullName(Long userId) {
        if (userId == null) {
            return "Unknown";
        }
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("Unknown");
    }

    private String lookupUserPhone(Long userId) {
        if (userId == null) {
            return "";
        }
        return userRepository.findById(userId)
                .map(User::getPhoneNumber)
                .orElse("");
    }
}
