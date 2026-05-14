package com.kumbukaa.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AuthDTO {
    private Long id;
    private Long userId;
    private String username;
    private String password;
    private String email;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
