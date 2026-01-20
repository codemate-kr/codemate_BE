package com.ryu.studyhelper.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "팀 초대 요청")
public record TeamJoinInviteRequest(
        @NotNull(message = "teamId는 필수입니다")
        @Schema(description = "팀 ID", example = "1")
        Long teamId,

        @NotNull(message = "targetMemberId는 필수입니다")
        @Schema(description = "초대할 멤버 ID", example = "2")
        Long targetMemberId
) {
}