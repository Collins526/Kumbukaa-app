package com.kumbukaa.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BorrowerDTO {
    private Long id;
    private Long userId;
    private Integer creditScore;
    private String idNumber;
    private String employmentStatus;
    private Double annualIncome;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String postalCode;
    private Boolean isVerified;
    private Double totalBorrowed;
    private Double totalRepaid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
