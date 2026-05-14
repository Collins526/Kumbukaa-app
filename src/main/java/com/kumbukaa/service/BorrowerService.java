package com.kumbukaa.service;

import com.kumbukaa.dto.BorrowerDTO;
import com.kumbukaa.entity.Borrower;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.BorrowerRepository;
import com.kumbukaa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BorrowerService {

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private UserRepository userRepository;

    public Borrower createBorrower(BorrowerDTO borrowerDTO) {
        if (borrowerDTO == null) throw new IllegalArgumentException("Borrower DTO cannot be null");
        Long userId = borrowerDTO.getUserId();
        if (userId == null) throw new IllegalArgumentException("User ID cannot be null");

        // Fetch the user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Check if borrower already exists for this user
        Optional<Borrower> existingBorrower = borrowerRepository.findByUserId(userId);
        Borrower borrower;
        if (existingBorrower.isPresent()) {
            // Update existing borrower
            borrower = existingBorrower.get();
        } else {
            // Create new borrower
            borrower = new Borrower();
            borrower.setUser(user);
        }

        // Map DTO properties to borrower entity
        borrower.setCreditScore(borrowerDTO.getCreditScore());
        borrower.setIdNumber(borrowerDTO.getIdNumber());
        borrower.setEmploymentStatus(borrowerDTO.getEmploymentStatus());
        borrower.setAnnualIncome(borrowerDTO.getAnnualIncome());
        borrower.setDateOfBirth(borrowerDTO.getDateOfBirth());
        borrower.setAddress(borrowerDTO.getAddress());
        borrower.setCity(borrowerDTO.getCity());
        borrower.setPostalCode(borrowerDTO.getPostalCode());
        borrower.setIsVerified(borrowerDTO.getIsVerified());
        borrower.setTotalBorrowed(borrowerDTO.getTotalBorrowed() != null ? borrowerDTO.getTotalBorrowed() : 0.0);
        borrower.setTotalRepaid(borrowerDTO.getTotalRepaid() != null ? borrowerDTO.getTotalRepaid() : 0.0);

        return borrowerRepository.save(borrower);
    }

    public Optional<Borrower> findById(Long id) {
        if (id == null) return Optional.empty();
        return borrowerRepository.findById(id);
    }

    public Optional<Borrower> findByUserId(Long userId) {
        return borrowerRepository.findByUserId(userId);
    }

    public Optional<Borrower> findByIdNumber(String idNumber) {
        return borrowerRepository.findByIdNumber(idNumber);
    }

    public List<Borrower> findAll() {
        return borrowerRepository.findAll();
    }

    public Borrower updateBorrower(Borrower borrower) {
        if (borrower == null) throw new IllegalArgumentException("Borrower cannot be null");
        return borrowerRepository.save(borrower);
    }

    public void deleteBorrower(Long id) {
        if (id != null) {
            borrowerRepository.deleteById(id);
        }
    }

    public void updateBorrowedAmount(Long borrowerId, Double amount) {
        if (borrowerId == null) return;
        Optional<Borrower> borrower = borrowerRepository.findById(borrowerId);
        if (borrower.isPresent()) {
            Borrower b = borrower.get();
            b.setTotalBorrowed(b.getTotalBorrowed() + amount);
            borrowerRepository.save(b);
        }
    }

    public void updateRepaidAmount(Long borrowerId, Double amount) {
        if (borrowerId == null) return;
        Optional<Borrower> borrower = borrowerRepository.findById(borrowerId);
        if (borrower.isPresent()) {
            Borrower b = borrower.get();
            b.setTotalRepaid(b.getTotalRepaid() + amount);
            borrowerRepository.save(b);
        }
    }
}
