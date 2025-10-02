package com.ryu.studyhelper.recommendation.domain;

/**
 * 추천 타입을 나타내는 열거형
 * SCHEDULED: 스케줄에 따른 자동 추천
 * MANUAL: 수동 추천 (팀장이 직접 요청)
 */
public enum RecommendationType {
    SCHEDULED,  // 스케줄 자동 추천
    MANUAL      // 수동 추천
}