package com.kumbukaa.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private String confirmPassword;
}
