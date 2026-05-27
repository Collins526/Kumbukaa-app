package com.kumbukaa.controller;

import com.kumbukaa.config.JwtTokenProvider;
import com.kumbukaa.dto.UpdateProfileRequest;
import com.kumbukaa.entity.User;
import com.kumbukaa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get current user profile
     * Requires: Valid JWT token in Authorization header
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return new ResponseEntity<>("Authorization token required", HttpStatus.UNAUTHORIZED);
            }

            String jwtToken = token.substring(7);
            
            // Validate token
            if (!jwtTokenProvider.validateToken(jwtToken)) {
                return new ResponseEntity<>("Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
            User user = userService.getUserProfile(userId);
            
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving profile: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Update user profile
     * Requires: Valid JWT token in Authorization header
     * Only logged-in users can update their own profile
     */
    @PutMapping("/profile/update")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody UpdateProfileRequest request) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return new ResponseEntity<>("Authorization token required", HttpStatus.UNAUTHORIZED);
            }

            String jwtToken = token.substring(7);
            
            // Validate token
            if (!jwtTokenProvider.validateToken(jwtToken)) {
                return new ResponseEntity<>("Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(jwtToken);
            User updatedUser = userService.updateProfile(userId, request);
            
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating profile: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get user by ID
     * Requires: Valid JWT token in Authorization header
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return new ResponseEntity<>("Authorization token required", HttpStatus.UNAUTHORIZED);
            }

            String jwtToken = token.substring(7);
            
            // Validate token
            if (!jwtTokenProvider.validateToken(jwtToken)) {
                return new ResponseEntity<>("Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            User user = userService.getUserProfile(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


}
