package com.kumbukaa.controller;

import com.kumbukaa.dto.AuthResponse;
import com.kumbukaa.dto.LoginRequest;
import com.kumbukaa.dto.OtpRequest;
import com.kumbukaa.dto.RegisterRequest;
import com.kumbukaa.dto.TokenRefreshRequest;
import com.kumbukaa.entity.Auth;
import com.kumbukaa.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            String message = authService.register(request);
            return new ResponseEntity<>(message, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new AuthResponse(null, null, null, null, null, false, e.getMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequest request) {
        try {
            String message = authService.requestOtp(request.getEmail());
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request.getRefreshToken());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new AuthResponse(null, null, null, null, null, false, e.getMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout/{authId}")
    public ResponseEntity<?> logout(@PathVariable Long authId) {
        try {
            authService.logout(authId);
            return new ResponseEntity<>("Logout successful", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return new ResponseEntity<>("Invalid token format", HttpStatus.BAD_REQUEST);
            }
            String jwtToken = token.substring(7);
            boolean isValid = authService.validateToken(jwtToken);
            if (isValid) {
                String email = authService.getUsernameFromToken(jwtToken);
                Long userId = authService.getUserIdFromToken(jwtToken);
                return new ResponseEntity<>(new AuthResponse(null, userId, email, null, null, true, "Token is valid"), HttpStatus.OK);
            }
            return new ResponseEntity<>("Token is invalid or expired", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAuthById(@PathVariable Long id) {
        Optional<Auth> auth = authService.findById(id);
        if (auth.isPresent()) {
            return new ResponseEntity<>(auth.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Auth not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAuthByUserId(@PathVariable Long userId) {
        Optional<Auth> auth = authService.findByUserId(userId);
        if (auth.isPresent()) {
            return new ResponseEntity<>(auth.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Auth record not found for user", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllAuth() {
        List<Auth> authList = authService.findAll();
        return new ResponseEntity<>(authList, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAuth(@PathVariable Long id, @RequestBody Auth auth) {
        Optional<Auth> existingAuth = authService.findById(id);
        if (existingAuth.isPresent()) {
            auth.setId(id);
            Auth updatedAuth = authService.updateAuth(auth);
            return new ResponseEntity<>(updatedAuth, HttpStatus.OK);
        }
        return new ResponseEntity<>("Auth not found", HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<?> verifyAuth(@PathVariable Long id) {
        boolean verified = authService.verifyAuth(id);
        if (verified) {
            return new ResponseEntity<>("Auth verified successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Auth not found", HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAuth(@PathVariable Long id) {
        Optional<Auth> auth = authService.findById(id);
        if (auth.isPresent()) {
            authService.deleteAuth(id);
            return new ResponseEntity<>("Auth deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Auth not found", HttpStatus.NOT_FOUND);
    }
}
