package com.kumbukaa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Long loansLent;
    private Long loansBorrowed;
}
