package com.ryu.studyhelper.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "팀 생성 요청")
public record CreateTeamRequest(
        @Schema(description = "팀 이름", example = "알고리즘 스터디")
        @NotBlank String name,

        @Schema(description = "팀 설명", example = "매주 문제를 풀어보는 스터디입니다.")
        String description,

        @Schema(description = "비공개 팀 여부 (기본값: false)", example = "false")
        Boolean isPrivate
) {}