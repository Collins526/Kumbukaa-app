package com.kumbukaa.dto;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAuditDto {
    private Long id;
    private String action, targetType, actorName;
    private Long targetId;
    private String targetName, details;
    private LocalDateTime createdAt;
}
