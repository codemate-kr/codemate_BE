package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자가 속한 팀 정보 응답")
public record MyTeamResponse(
        @Schema(description = "팀 ID", example = "1")
        Long teamId,

        @Schema(description = "팀 이름", example = "알고리즘 스터디")
        String teamName,

        @Schema(description = "팀 설명", example = "매주 문제를 풀어보는 스터디입니다.")
        String teamDescription,

        @Schema(description = "사용자의 팀 내 역할", example = "LEADER")
        TeamRole myRole,

        @Schema(description = "팀원 수", example = "5")
        int memberCount,

        @Schema(description = "비공개 팀 여부", example = "false")
        boolean isPrivate,

        @Schema(description = "팀 생성일시")
        LocalDateTime createdAt
) {
    public static MyTeamResponse from(TeamMember teamMember) {
        Team team = teamMember.getTeam();
        return new MyTeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                teamMember.getRole(),
                team.getTeamMembers().size(),
                team.getIsPrivate(),
                team.getCreatedAt()
        );
    }
}
