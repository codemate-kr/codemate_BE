package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공개 팀 정보 응답")
public record PublicTeamResponse(
        @Schema(description = "팀 ID", example = "1")
        Long teamId,

        @Schema(description = "팀 이름", example = "알고리즘 스터디")
        String teamName,

        @Schema(description = "팀 설명", example = "매주 알고리즘 문제를 함께 풀어보는 스터디입니다.")
        String description,

        @Schema(description = "팀장 BOJ 핸들", example = "tourist")
        String leaderHandle,

        @Schema(description = "팀원 수", example = "5")
        int memberCount
) {
    public static PublicTeamResponse from(Team team) {
        String leaderHandle = team.getTeamMembers().stream()
                .filter(tm -> tm.getRole() == TeamRole.LEADER)
                .findFirst()
                .map(tm -> tm.getMember().getHandle())
                .orElse(null);

        return new PublicTeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                leaderHandle,
                team.getTeamMembers().size()
        );
    }
}
