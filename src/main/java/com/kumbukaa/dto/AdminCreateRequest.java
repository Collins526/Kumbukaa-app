package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String password;
    private String confirmPassword;
}
