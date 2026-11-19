package com.kumbukaa.dto;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDto {
    private Long id;
    private String title, message;
    private LocalDateTime createdAt;
    private Boolean read;
}
