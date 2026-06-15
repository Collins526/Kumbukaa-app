package com.kumbukaa.controller;

import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.LoginWithOtpRequest;
import com.kumbukaa.dto.OtpRequest;
import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        }
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.adminLogin(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        }
    }

    @PostMapping("/request-otp")
    public ResponseEntity<String> requestOtp(@RequestBody OtpRequest request) {
        try {
            return ResponseEntity.ok(authService.requestOtp(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login-otp")
    public ResponseEntity<AuthResponse> loginWithOtp(@RequestBody LoginWithOtpRequest request) {
        try {
            return ResponseEntity.ok(authService.loginWithOtp(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthResponse(null, null, null, e.getMessage(), null, null));
        }
    }
}
