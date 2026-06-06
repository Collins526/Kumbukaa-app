package com.kumbukaa.controller;

import com.kumbukaa.dto.LoanLentRequest;
import com.kumbukaa.dto.PaymentRequest;
import com.kumbukaa.entity.LoanLent;
import com.kumbukaa.service.LoanLentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans-lent")
public class LoanLentController {

    private final LoanLentService service;

    public LoanLentController(LoanLentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LoanLent> createLoan(@RequestBody LoanLentRequest request) {
        return new ResponseEntity<>(service.createLoan(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LoanLent>> getAllLoans() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanLent> getLoanById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanLent> updateLoan(@PathVariable Long id, @RequestBody LoanLentRequest request) {
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
    public ResponseEntity<LoanLent> recordPayment(@PathVariable Long id, @RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.ok(service.recordPayment(id, request));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }
}
