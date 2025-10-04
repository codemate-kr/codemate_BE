package com.ryu.studyhelper.member.verification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 백준 핸들 검증 요청 DTO
 */
public record VerifyBojRequest(
        @NotBlank(message = "백준 핸들은 필수입니다.")
        String handle
) {}