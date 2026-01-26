package com.ryu.studyhelper.notification.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.notification.domain.Notification;
import com.ryu.studyhelper.notification.domain.NotificationType;
import com.ryu.studyhelper.notification.dto.NotificationListResponse;
import com.ryu.studyhelper.notification.dto.NotificationResponse;
import com.ryu.studyhelper.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long memberId, Long cursor, int size) {
        Slice<Notification> slice = notificationRepository.findByRecipientId(
                memberId, cursor, PageRequest.of(0, size));

        return NotificationListResponse.of(
                slice.getContent().stream().map(NotificationResponse::from).toList(),
                slice.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getUnreadNotifications(Long memberId, Long cursor, int size) {
        Slice<Notification> slice = notificationRepository.findUnreadByRecipientId(
                memberId, cursor, PageRequest.of(0, size));

        return NotificationListResponse.of(
                slice.getContent().stream().map(NotificationResponse::from).toList(),
                slice.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return notificationRepository.countUnreadByRecipientId(memberId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.NOTIFICATION_NOT_FOUND));

        if (notification.getRecipient() == null || !notification.getRecipient().getId().equals(memberId)) {
            throw new CustomException(CustomResponseStatus.FORBIDDEN);
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsRead(memberId);
    }

    @Transactional
    public void delete(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.NOTIFICATION_NOT_FOUND));

        if (notification.getRecipient() == null || !notification.getRecipient().getId().equals(memberId)) {
            throw new CustomException(CustomResponseStatus.FORBIDDEN);
        }

        notification.softDelete();
        notificationRepository.save(notification);
    }

    @Transactional
    public Notification createNotification(Long recipientId, NotificationType type, Map<String, Object> metadata) {
        Member recipient = memberRepository.findById(recipientId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        Notification notification = Notification.create(recipient, type, metadata);
        return notificationRepository.save(notification);
    }
}