package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;

public record TeamMemberResponse(
        Long memberId,
        String handle,
        String email,
        TeamRole role,
        boolean isMe
) {
    public static TeamMemberResponse from(TeamMember teamMember, Long currentMemberId) {
        return new TeamMemberResponse(
                teamMember.getMember().getId(),
                teamMember.getMember().getHandle(),
                maskEmail(teamMember.getMember().getEmail()),
                teamMember.getRole(),
                teamMember.getMember().getId().equals(currentMemberId)
        );
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 4) {
            return "****@" + domain;
        }

        String visiblePart = localPart.substring(0, localPart.length() - 4);
        return visiblePart + "****@" + domain;
    }
}