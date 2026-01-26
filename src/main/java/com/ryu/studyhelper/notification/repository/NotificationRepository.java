package com.ryu.studyhelper.notification.repository;

import com.ryu.studyhelper.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 전체 알림 (삭제 안된 것, cursor 기반 페이징)
    @Query("""
           SELECT n FROM Notification n
           WHERE n.recipient.id = :recipientId
             AND n.deletedAt IS NULL
             AND (:cursor IS NULL OR n.id < :cursor)
           ORDER BY n.id DESC
           """)
    Slice<Notification> findByRecipientId(Long recipientId, Long cursor, Pageable pageable);

    // 읽지 않은 알림 (cursor 기반 페이징)
    @Query("""
           SELECT n FROM Notification n
           WHERE n.recipient.id = :recipientId
             AND n.readAt IS NULL
             AND n.deletedAt IS NULL
             AND (:cursor IS NULL OR n.id < :cursor)
           ORDER BY n.id DESC
           """)
    Slice<Notification> findUnreadByRecipientId(Long recipientId, Long cursor, Pageable pageable);

    // 읽지 않은 개수
    @Query("""
           SELECT COUNT(n) FROM Notification n
           WHERE n.recipient.id = :recipientId
             AND n.readAt IS NULL
             AND n.deletedAt IS NULL
           """)
    long countUnreadByRecipientId(Long recipientId);

    // 전체 읽음 처리
    @Modifying
    @Query("""
           UPDATE Notification n
           SET n.readAt = CURRENT_TIMESTAMP
           WHERE n.recipient.id = :recipientId
             AND n.readAt IS NULL
             AND n.deletedAt IS NULL
           """)
    int markAllAsRead(Long recipientId);
}