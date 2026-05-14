package com.kumbukaa.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    private String email;
    private String password;
    private String otp;
}
