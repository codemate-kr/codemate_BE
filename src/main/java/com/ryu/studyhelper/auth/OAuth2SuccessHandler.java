package com.ryu.studyhelper.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryu.studyhelper.auth.token.RefreshTokenService;
import com.ryu.studyhelper.config.jwt.util.JwtUtil;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Member member = principalDetails.getMember();

        // 1. JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(
                member.getId(),
                member.getRole().getValue()
        );

        String refreshToken = jwtUtil.createRefreshToken(member.getId());

        // 2. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(member.getId(), refreshToken);

        // 3. 사용자 상태에 따라 리다이렉트 결정
        String redirectUrl = determineRedirectUrl(member, accessToken, refreshToken);

        log.info("OAuth2 login success for user: {}", member.getEmail());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String determineRedirectUrl(Member member, String accessToken, String refreshToken) {
        String baseUrl = "http://localhost:3000";
        String tokenParams = String.format("?access_token=%s&refresh_token=%s", accessToken, refreshToken);
        log.info("Generated tokens - Access: {}, Refresh: {}", accessToken, refreshToken);
        if (!member.isVerified()) {
            // 백준 인증 미완료 → 인증 페이지로
            return baseUrl + "/auth/verify-boj" + tokenParams;
        } else {
            // 인증 완료 → 대시보드로
            return baseUrl + "/dashboard" + tokenParams;
        }
    }
}