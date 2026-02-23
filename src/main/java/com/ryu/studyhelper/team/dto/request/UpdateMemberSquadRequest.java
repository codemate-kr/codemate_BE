package com.ryu.studyhelper.team.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberSquadRequest(
        @NotNull(message = "squadId는 필수입니다.")
        Long squadId
) {}
