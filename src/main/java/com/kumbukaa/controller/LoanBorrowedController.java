package com.kumbukaa.controller;

import com.kumbukaa.dto.LoanBorrowedRequest;
import com.kumbukaa.dto.PaymentRequest;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.service.LoanBorrowedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans-borrowed")
public class LoanBorrowedController {

    private final LoanBorrowedService service;

    public LoanBorrowedController(LoanBorrowedService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LoanBorrowed> createLoan(@RequestBody LoanBorrowedRequest request) {
        return new ResponseEntity<>(service.createLoan(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LoanBorrowed>> getAllLoans() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanBorrowed> getLoanById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanBorrowed> updateLoan(@PathVariable Long id, @RequestBody LoanBorrowedRequest request) {
        try {
            return ResponseEntity.ok(service.updateLoan(id, request));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        service.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<LoanBorrowed> recordPayment(@PathVariable Long id, @RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(service.recordPayment(id, request));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }
}
