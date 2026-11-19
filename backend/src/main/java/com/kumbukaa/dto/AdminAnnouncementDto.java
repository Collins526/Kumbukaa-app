package com.kumbukaa.dto;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAnnouncementDto {
    private Long id;
    private String title, message, audience, channel;
    private Long targetUserId;
    private String targetName;
    private LocalDateTime createdAt;
    private String authorName;
    private Integer recipientCount;
}
