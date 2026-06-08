package com.kumbukaa.controller;

import com.kumbukaa.dto.LoanBorrowedRequest;
import com.kumbukaa.dto.PaymentRequest;
import com.kumbukaa.entity.LoanBorrowed;
import com.kumbukaa.service.LoanBorrowedService;
import com.kumbukaa.util.SecurityUtils;
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
        Long userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(service.createLoan(request, userId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LoanBorrowed>> getAllLoans() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.findAll(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanBorrowed> getLoanById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return service.findById(id, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanBorrowed> updateLoan(@PathVariable Long id, @RequestBody LoanBorrowedRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            return ResponseEntity.ok(service.updateLoan(id, request, userId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        service.deleteLoan(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<LoanBorrowed> recordPayment(@PathVariable Long id, @RequestBody PaymentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            return ResponseEntity.ok(service.recordPayment(id, request, userId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }
}
