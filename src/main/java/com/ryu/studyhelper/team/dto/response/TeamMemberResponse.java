package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.common.util.MaskingUtils;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;

public record TeamMemberResponse(
        Long memberId,
        String handle,
        String email,
        TeamRole role,
        Long squadId,
        String squadName,
        boolean isMe
) {
    public static TeamMemberResponse from(TeamMember teamMember, Long currentMemberId, String squadName) {
        return new TeamMemberResponse(
                teamMember.getMember().getId(),
                teamMember.getMember().getHandle(),
                MaskingUtils.maskEmail(teamMember.getMember().getEmail()),
                teamMember.getRole(),
                teamMember.getSquadId(),
                squadName,
                teamMember.getMember().getId().equals(currentMemberId)
        );
    }
}
