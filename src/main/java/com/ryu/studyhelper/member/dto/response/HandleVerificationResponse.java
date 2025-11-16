package com.ryu.studyhelper.member.dto.response;

import com.ryu.studyhelper.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "백준 핸들 인증 응답")
public record HandleVerificationResponse(
        @Schema(description = "인증된 백준 핸들", example = "algorithm_master")
        String handle
) {
    public static HandleVerificationResponse from(Member member) {
        return new HandleVerificationResponse(member.getHandle());
    }
}