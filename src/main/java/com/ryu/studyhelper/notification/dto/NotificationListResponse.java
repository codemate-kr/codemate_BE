package com.ryu.studyhelper.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        Long nextCursor,
        boolean hasNext
) {
    public static NotificationListResponse of(List<NotificationResponse> notifications, boolean hasNext) {
        Long nextCursor = hasNext && !notifications.isEmpty()
                ? notifications.get(notifications.size() - 1).id()
                : null;
        return new NotificationListResponse(notifications, nextCursor, hasNext);
    }
}