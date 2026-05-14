package com.kumbukaa.controller;

import com.kumbukaa.entity.Transaction;
import com.kumbukaa.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Transaction> logPayment(
            @RequestParam Long loanId,
            @RequestParam Double amount,
            @RequestParam String mpesaCode) {

        return new ResponseEntity<>(service.logPayment(loanId, amount, mpesaCode), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Transaction> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approveTransaction(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        Optional<Transaction> tx = service.findById(id);
        return tx.map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<Transaction>> getTransactionsByLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(service.findByLoanId(loanId));
    }

    @GetMapping("/mpesa/{mpesaCode}")
    public ResponseEntity<?> getTransactionByMpesaCode(@PathVariable String mpesaCode) {
        Optional<Transaction> tx = service.findByMpesaTransactionId(mpesaCode);
        return tx.map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id, @RequestBody Transaction tx) {
        try {
            tx.setId(id);
            return ResponseEntity.ok(service.updateTransaction(tx));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        service.deleteTransaction(id);
        return ResponseEntity.ok("Transaction deleted successfully");
    }
}
