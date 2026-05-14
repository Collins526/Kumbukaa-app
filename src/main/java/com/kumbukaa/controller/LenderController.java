package com.kumbukaa.controller;


import com.kumbukaa.entity.Lender;
import com.kumbukaa.service.LenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lender")
public class LenderController {

    @Autowired
    private LenderService lenderService;

    @PostMapping("/create")
    public ResponseEntity<?> createLender(@RequestBody Lender lender) {
        try {
            Lender createdLender = lenderService.createLender(lender);
            return new ResponseEntity<>(createdLender, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLenderById(@PathVariable Long id) {
        Optional<Lender> lender = lenderService.findById(id);
        if (lender.isPresent()) {
            return new ResponseEntity<>(lender.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Lender not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getLenderByUserId(@PathVariable Long userId) {
        Optional<Lender> lender = lenderService.findByUserId(userId);
        if (lender.isPresent()) {
            return new ResponseEntity<>(lender.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Lender record not found for user", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllLenders() {
        List<Lender> lenders = lenderService.findAll();
        return new ResponseEntity<>(lenders, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLender(@PathVariable Long id, @RequestBody Lender lender) {
        Optional<Lender> existingLender = lenderService.findById(id);
        if (existingLender.isPresent()) {
            lender.setId(id);
            Lender updatedLender = lenderService.updateLender(lender);
            return new ResponseEntity<>(updatedLender, HttpStatus.OK);
        }
        return new ResponseEntity<>("Lender not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLender(@PathVariable Long id) {
        Optional<Lender> lender = lenderService.findById(id);
        if (lender.isPresent()) {
            lenderService.deleteLender(id);
            return new ResponseEntity<>("Lender deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Lender not found", HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{id}/lent/{amount}")
    public ResponseEntity<?> updateLentAmount(@PathVariable Long id, @PathVariable Double amount) {
        try {
            lenderService.updateLentAmount(id, amount);
            return new ResponseEntity<>("Lent amount updated", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}/recovered/{amount}")
    public ResponseEntity<?> updateRecoveredAmount(@PathVariable Long id, @PathVariable Double amount) {
        try {
            lenderService.updateRecoveredAmount(id, amount);
            return new ResponseEntity<>("Recovered amount updated", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
