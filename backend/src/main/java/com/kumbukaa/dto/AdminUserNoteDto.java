package com.kumbukaa.dto;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserNoteDto {
    private Long id;
    private String note, authorName;
    private LocalDateTime createdAt;
}
