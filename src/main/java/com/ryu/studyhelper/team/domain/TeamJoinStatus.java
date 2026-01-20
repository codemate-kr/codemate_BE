package com.ryu.studyhelper.team.domain;

public enum TeamJoinStatus {
    PENDING,    // 대기 중
    ACCEPTED,   // 수락됨
    REJECTED,   // 거절됨
    CANCELED,   // 취소됨
    EXPIRED     // 만료됨
}