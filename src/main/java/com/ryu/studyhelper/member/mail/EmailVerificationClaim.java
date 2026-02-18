package com.ryu.studyhelper.member.mail;

/**
 * 이메일 인증 토큰에서 추출한 클레임
 */
public record EmailVerificationClaim(
        Long memberId,
        String newEmail
) {
}