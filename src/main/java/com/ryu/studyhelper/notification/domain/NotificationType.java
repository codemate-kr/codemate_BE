package com.ryu.studyhelper.notification.domain;

public enum NotificationType {
    // 팀 초대 (팀장 → 멤버)
    TEAM_INVITATION,
    TEAM_INVITATION_ACCEPTED,
    TEAM_INVITATION_REJECTED,

    // 팀 가입 신청 (멤버 → 팀)
    TEAM_APPLICATION,
    TEAM_APPLICATION_ACCEPTED,
    TEAM_APPLICATION_REJECTED,

    // 팀 멤버 변동
    MEMBER_LEFT,
    MEMBER_JOINED
}