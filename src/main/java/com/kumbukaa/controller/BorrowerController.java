package com.kumbukaa.controller;


import com.kumbukaa.dto.BorrowerDTO;
import com.kumbukaa.entity.Borrower;
import com.kumbukaa.service.BorrowerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/borrower")
public class BorrowerController {

    @Autowired
    private BorrowerService borrowerService;

    @PostMapping("/create")
    public ResponseEntity<?> createBorrower(@RequestBody BorrowerDTO borrowerDTO) {
        try {
            Borrower createdBorrower = borrowerService.createBorrower(borrowerDTO);
            return new ResponseEntity<>(createdBorrower, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBorrowerById(@PathVariable Long id) {
        Optional<Borrower> borrower = borrowerService.findById(id);
        if (borrower.isPresent()) {
            return new ResponseEntity<>(borrower.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Borrower not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBorrowerByUserId(@PathVariable Long userId) {
        Optional<Borrower> borrower = borrowerService.findByUserId(userId);
        if (borrower.isPresent()) {
            return new ResponseEntity<>(borrower.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Borrower record not found for user", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/id/{idNumber}")
    public ResponseEntity<?> getBorrowerByIdNumber(@PathVariable String idNumber) {
        Optional<Borrower> borrower = borrowerService.findByIdNumber(idNumber);
        if (borrower.isPresent()) {
            return new ResponseEntity<>(borrower.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Borrower with ID number not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllBorrowers() {
        List<Borrower> borrowers = borrowerService.findAll();
        return new ResponseEntity<>(borrowers, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBorrower(@PathVariable Long id, @RequestBody Borrower borrower) {
        Optional<Borrower> existingBorrower = borrowerService.findById(id);
        if (existingBorrower.isPresent()) {
            borrower.setId(id);
            Borrower updatedBorrower = borrowerService.updateBorrower(borrower);
            return new ResponseEntity<>(updatedBorrower, HttpStatus.OK);
        }
        return new ResponseEntity<>("Borrower not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBorrower(@PathVariable Long id) {
        Optional<Borrower> borrower = borrowerService.findById(id);
        if (borrower.isPresent()) {
            borrowerService.deleteBorrower(id);
            return new ResponseEntity<>("Borrower deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Borrower not found", HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{id}/borrowed/{amount}")
    public ResponseEntity<?> updateBorrowedAmount(@PathVariable Long id, @PathVariable Double amount) {
        try {
            borrowerService.updateBorrowedAmount(id, amount);
            return new ResponseEntity<>("Borrowed amount updated", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}/repaid/{amount}")
    public ResponseEntity<?> updateRepaidAmount(@PathVariable Long id, @PathVariable Double amount) {
        try {
            borrowerService.updateRepaidAmount(id, amount);
            return new ResponseEntity<>("Repaid amount updated", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
