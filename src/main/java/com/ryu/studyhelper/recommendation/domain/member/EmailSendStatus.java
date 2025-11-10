package com.ryu.studyhelper.recommendation.domain.member;

/**
 * 이메일 발송 상태를 나타내는 열거형
 * MemberRecommendation에서 개인별 이메일 발송 상태를 관리합니다.
 *
 * PENDING: 이메일 발송 대기 중
 * SENT: 이메일 발송 완료
 * FAILED: 이메일 발송 실패 (재시도 가능)
 */
public enum EmailSendStatus {
    PENDING,  // 발송 대기 중
    SENT,     // 발송 완료
    FAILED    // 발송 실패
}