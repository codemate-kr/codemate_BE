package com.ryu.studyhelper.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "팀 공개/비공개 설정 변경 요청")
public record UpdateTeamVisibilityRequest(
        @Schema(description = "비공개 여부 (true: 비공개, false: 공개)", example = "true")
        @NotNull(message = "공개/비공개 설정은 필수입니다")
        Boolean isPrivate
) {
}