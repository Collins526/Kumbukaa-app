package com.kumbukaa.controller;

import com.kumbukaa.entity.Loan;
import com.kumbukaa.service.LoanService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Loan> createLoan(
            @RequestParam Long lenderId,
            @RequestParam Long borrowerId,
            @RequestParam Double amount) {

        return new ResponseEntity<>(service.createLoan(lenderId, borrowerId, amount), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Loan> acceptLoan(@PathVariable Long id) {
        return ResponseEntity.ok(service.acceptLoan(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Loan> rejectLoan(@PathVariable Long id) {
        return ResponseEntity.ok(service.rejectLoan(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanById(@PathVariable Long id) {
        Optional<Loan> loan = service.findById(id);
        return loan.map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Loan>> getAllLoans() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/lender/{lenderId}")
    public ResponseEntity<List<Loan>> getLoansByLender(@PathVariable Long lenderId) {
        return ResponseEntity.ok(service.findByLenderId(lenderId));
    }

    @GetMapping("/borrower/{borrowerId}")
    public ResponseEntity<List<Loan>> getLoansByBorrower(@PathVariable Long borrowerId) {
        return ResponseEntity.ok(service.findByBorrowerId(borrowerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLoan(@PathVariable Long id, @RequestBody Loan loan) {
        try {
            loan.setId(id);
            return ResponseEntity.ok(service.updateLoan(loan));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLoan(@PathVariable Long id) {
        service.deleteLoan(id);
        return ResponseEntity.ok("Loan deleted successfully");
    }
}
