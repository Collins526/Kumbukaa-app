package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String name;
    private String email;
    private String phoneNumber;
}
