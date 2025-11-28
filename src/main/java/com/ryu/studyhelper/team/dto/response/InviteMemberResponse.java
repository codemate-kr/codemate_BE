package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 멤버 초대 응답 DTO
 */
@Schema(description = "멤버 초대 응답")
public record InviteMemberResponse(
        @Schema(description = "팀원 ID", example = "1")
        Long teamMemberId,

        @Schema(description = "초대된 멤버의 이메일", example = "member@example.com")
        String email,

        @Schema(description = "초대된 멤버의 handle", example = "codemate")
        String handle,

        @Schema(description = "팀 이름", example = "알고리즘 스터디")
        String teamName
) {
    public static InviteMemberResponse from(TeamMember teamMember) {
        return new InviteMemberResponse(
                teamMember.getId(),
                teamMember.getMember().getEmail(),
                teamMember.getMember().getHandle(),
                teamMember.getTeam().getName()
        );
    }
}