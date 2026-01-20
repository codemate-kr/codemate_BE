package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.TeamJoin;
import com.ryu.studyhelper.team.domain.TeamJoinStatus;
import com.ryu.studyhelper.team.domain.TeamJoinType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "팀 초대/가입 요청 응답")
public record TeamJoinResponse(
        @Schema(description = "요청 ID")
        Long id,

        @Schema(description = "팀 ID")
        Long teamId,

        @Schema(description = "팀 이름")
        String teamName,

        @Schema(description = "요청 유형")
        TeamJoinType type,

        @Schema(description = "요청자 핸들")
        String requesterHandle,

        @Schema(description = "대상 멤버 핸들")
        String targetMemberHandle,

        @Schema(description = "상태")
        TeamJoinStatus status,

        @Schema(description = "만료 시간")
        LocalDateTime expiresAt,

        @Schema(description = "생성 시간")
        LocalDateTime createdAt
) {
    public static TeamJoinResponse from(TeamJoin teamJoin) {
        return new TeamJoinResponse(
                teamJoin.getId(),
                teamJoin.getTeam().getId(),
                teamJoin.getTeam().getName(),
                teamJoin.getType(),
                teamJoin.getRequester().getHandle(),
                teamJoin.getTargetMember() != null ? teamJoin.getTargetMember().getHandle() : null,
                teamJoin.getStatus(),
                teamJoin.getExpiresAt(),
                teamJoin.getCreatedAt()
        );
    }
}