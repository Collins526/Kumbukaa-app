package com.kumbukaa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private List<LoanResponse> loansLent;
    private List<LoanResponse> loansBorrowed;
}
