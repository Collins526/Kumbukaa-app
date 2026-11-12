package com.kumbukaa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAdminDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private List<LoanResponse> loansLent;
    private List<LoanResponse> loansBorrowed;
}
