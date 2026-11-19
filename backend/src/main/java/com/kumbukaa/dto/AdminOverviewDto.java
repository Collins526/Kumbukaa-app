package com.kumbukaa.dto;
import lombok.*;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminOverviewDto {
    private long totalAccounts, activeAccounts, totalLoans, overdueLoans;
    private double totalLent, outstandingBalance, overdueAmount;
    private List<MonthlyVolume> monthlyVolume;
    private StatusCounts statusCounts;
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthlyVolume { private String label; private double lent, repaid; }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatusCounts { private long active, overdue, repaid; }
}
