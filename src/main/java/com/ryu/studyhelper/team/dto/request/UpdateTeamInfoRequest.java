package com.ryu.studyhelper.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "팀 정보 수정 요청")
public record UpdateTeamInfoRequest(
        @Schema(description = "팀 이름", example = "알고리즘 스터디")
        @NotBlank(message = "팀 이름은 필수입니다")
        String name,

        @Schema(description = "팀 설명", example = "매주 문제를 풀어보는 스터디입니다.")
        String description,

        @Schema(description = "비공개 여부 (true: 비공개, false: 공개)", example = "false")
        Boolean isPrivate
) {}
