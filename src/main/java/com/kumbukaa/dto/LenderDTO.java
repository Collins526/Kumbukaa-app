package com.kumbukaa.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LenderDTO {
    private Long id;
    private Long userId;
    private String lenderName;
    private Double availableCapital;
    private Double totalLent;
    private Double totalRecovered;
    private Double interestRate;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String postalCode;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
