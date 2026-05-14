package com.kumbukaa.controller;

import com.kumbukaa.repository.AuthRepository;
import com.kumbukaa.repository.BorrowerRepository;
import com.kumbukaa.repository.LenderRepository;
import com.kumbukaa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private LenderRepository lenderRepository;

    @GetMapping("/dump")
    public ResponseEntity<?> dumpAllTables() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("auth", authRepository.findAll());
        payload.put("users", userRepository.findAll());
        payload.put("borrowers", borrowerRepository.findAll());
        payload.put("lenders", lenderRepository.findAll());
        return new ResponseEntity<>(payload, HttpStatus.OK);
    }
}
