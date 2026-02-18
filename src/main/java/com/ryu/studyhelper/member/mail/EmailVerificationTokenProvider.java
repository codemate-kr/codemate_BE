package com.ryu.studyhelper.member.mail;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증 토큰 생성 및 검증
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationTokenProvider {

    private final JwtUtil jwtUtil;

    public String createToken(Long memberId, String email) {
        return jwtUtil.createEmailVerificationToken(memberId, email);
    }

    public EmailVerificationClaim parseToken(String token) {
        jwtUtil.validateTokenOrThrow(token);

        String tokenType = jwtUtil.getTokenType(token);
        if (!JwtUtil.TOKEN_TYPE_EMAIL_VERIFICATION.equals(tokenType)) {
            throw new CustomException(CustomResponseStatus.INVALID_EMAIL_VERIFICATION_TOKEN);
        }

        Long memberId = jwtUtil.getIdFromToken(token);
        String newEmail = jwtUtil.getEmailFromToken(token);

        return new EmailVerificationClaim(memberId, newEmail);
    }
}