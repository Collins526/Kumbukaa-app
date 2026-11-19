package com.kumbukaa.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AnnouncementRequest {
    private String title, message, audience, channel;
    private Long targetUserId;
}
