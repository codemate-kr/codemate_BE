package com.ryu.studyhelper.recommendation.domain.team;

/**
 * 추천 상태를 나타내는 열거형
 * PENDING: 추천 생성됨, 이메일 발송 대기 중
 * SENT: 이메일 발송 완료
 * FAILED: 이메일 발송 실패
 */
public enum RecommendationStatus {
    PENDING,  // 발송 대기 중
    SENT,     // 발송 완료
    FAILED    // 발송 실패
}