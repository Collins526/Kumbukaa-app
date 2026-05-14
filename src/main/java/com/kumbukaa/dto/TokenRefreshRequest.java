package com.kumbukaa.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TokenRefreshRequest {
    private String refreshToken;
}
