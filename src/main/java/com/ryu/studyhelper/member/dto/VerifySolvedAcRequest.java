package com.ryu.studyhelper.member.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifySolvedAcRequest(
        @NotBlank String handle
) {}

