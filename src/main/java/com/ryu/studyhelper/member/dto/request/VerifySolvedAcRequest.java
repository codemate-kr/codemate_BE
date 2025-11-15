package com.ryu.studyhelper.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifySolvedAcRequest(
        @NotBlank String handle
) {}

