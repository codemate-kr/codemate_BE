package com.ryu.studyhelper.member.verification.dto;

/**
 * 백준 핸들 검증 응답 DTO
 */
public record VerifyBojResponse(
        boolean verified,
        String handle,
        String message
) {
    public static VerifyBojResponse success(String handle) {
        return new VerifyBojResponse(
                true,
                handle,
                "백준 핸들 인증이 완료되었습니다."
        );
    }

    public static VerifyBojResponse failure(String reason) {
        return new VerifyBojResponse(
                false,
                null,
                reason
        );
    }
}