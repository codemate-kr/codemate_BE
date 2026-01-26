package com.ryu.studyhelper.notification.controller;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.notification.dto.NotificationListResponse;
import com.ryu.studyhelper.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "전체 알림 조회", description = "삭제되지 않은 전체 알림을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        NotificationListResponse response = notificationService.getNotifications(
                principalDetails.getMemberId(), cursor, size);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "읽지 않은 알림 조회", description = "읽지 않은 알림만 조회합니다.")
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<NotificationListResponse>> getUnreadNotifications(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {

        NotificationListResponse response = notificationService.getUnreadNotifications(
                principalDetails.getMemberId(), cursor, size);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "읽지 않은 알림 개수", description = "읽지 않은 알림 개수를 조회합니다.")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        long count = notificationService.getUnreadCount(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(count, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "개별 읽음 처리", description = "알림을 읽음 처리합니다.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long id) {

        notificationService.markAsRead(id, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "전체 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        notificationService.markAllAsRead(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "알림 삭제", description = "알림을 삭제합니다. (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long id) {

        notificationService.delete(id, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }
}