package com.ryu.studyhelper.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSquadRequest(
        @NotBlank(message = "스쿼드 이름은 필수입니다.")
        @Size(max = 255, message = "스쿼드 이름은 255자 이하여야 합니다.")
        String name,

        @Size(max = 255, message = "스쿼드 설명은 255자 이하여야 합니다.")
        String description
) {}
