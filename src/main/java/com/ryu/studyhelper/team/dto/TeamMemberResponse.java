package com.ryu.studyhelper.team.dto;

import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;

public record TeamMemberResponse(
        Long memberId,
        String handle,
        String email,
        TeamRole role
) {
    public static TeamMemberResponse from(TeamMember teamMember) {
        return new TeamMemberResponse(
                teamMember.getMember().getId(),
                teamMember.getMember().getHandle(),
                teamMember.getMember().getEmail(),
                teamMember.getRole()
        );
    }
}