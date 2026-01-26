package com.ryu.studyhelper.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    TEAM_INVITATION(List.of("teamId", "teamName", "inviterId", "inviterName")),
    TEAM_INVITATION_ACCEPTED(List.of("teamId", "teamName", "memberId", "memberName")),
    TEAM_INVITATION_REJECTED(List.of("teamId", "teamName", "memberId", "memberName")),

    TEAM_APPLICATION(List.of("teamId", "teamName", "applicantId", "applicantName")),
    TEAM_APPLICATION_ACCEPTED(List.of("teamId", "teamName")),
    TEAM_APPLICATION_REJECTED(List.of("teamId", "teamName")),

    MEMBER_LEFT(List.of("teamId", "teamName", "memberId", "memberName")),
    MEMBER_JOINED(List.of("teamId", "teamName", "memberId", "memberName"));

    private final List<String> requiredFields;
}