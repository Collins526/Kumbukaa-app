package com.kumbukaa.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @JsonAlias("fullName")
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private String confirmPassword;
}
