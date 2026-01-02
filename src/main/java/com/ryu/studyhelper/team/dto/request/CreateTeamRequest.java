package com.ryu.studyhelper.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "팀 생성 요청")
public record CreateTeamRequest(
        @Schema(description = "팀 이름 (최대 50자)", example = "알고리즘 스터디")
        @NotBlank(message = "팀 이름은 필수입니다")
        @Size(max = 50, message = "팀 이름은 50자 이내로 입력해주세요")
        String name,

        @Schema(description = "팀 설명 (최대 200자)", example = "매주 문제를 풀어보는 스터디입니다.")
        @Size(max = 200, message = "팀 설명은 200자 이내로 입력해주세요")
        String description,

        @Schema(description = "비공개 팀 여부 (기본값: false)", example = "false")
        Boolean isPrivate
) {}