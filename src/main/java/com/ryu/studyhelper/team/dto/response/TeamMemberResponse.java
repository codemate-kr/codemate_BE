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
                teamMember.getMember().getEmail(),
                teamMember.getRole(),
                teamMember.getMember().getId().equals(currentMemberId)
        );
    }
}