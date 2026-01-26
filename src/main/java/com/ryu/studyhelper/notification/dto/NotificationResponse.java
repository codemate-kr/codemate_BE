package com.ryu.studyhelper.notification.dto;

import com.ryu.studyhelper.notification.domain.Notification;
import com.ryu.studyhelper.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
        Long id,
        NotificationType type,
        Map<String, Object> metadata,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMetadata(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}