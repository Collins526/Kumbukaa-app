package com.kumbukaa.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private Long id;
    private Long userId;
    private String email;
    private String token;
    private String refreshToken;
    private Boolean isVerified;
    private String message;
}
