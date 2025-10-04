package com.ryu.studyhelper.member.verification.dto;

/**
 * 인증 해시 생성 응답 DTO
 */
public record GenerateHashResponse(
        String verificationHash,
        String message
) {
    public static GenerateHashResponse of(String hash) {
        return new GenerateHashResponse(
                hash,
                "solved.ac 프로필의 상태 메시지에 이 해시를 입력해주세요."
        );
    }
}