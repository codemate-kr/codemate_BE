package com.ryu.studyhelper.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryu.studyhelper.auth.token.RefreshCookieManager;
import com.ryu.studyhelper.auth.token.RefreshTokenService;
import com.ryu.studyhelper.config.security.jwt.JwtUtil;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.member.MemberService;
import com.ryu.studyhelper.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final RefreshCookieManager refreshCookieManager;
    private final ObjectMapper objectMapper;
    private final MemberService memberService;

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

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
                member.getRole().name()
        );

        String refreshToken = jwtUtil.createRefreshToken(member.getId());

        // 2. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(member.getId(), refreshToken);

        // 3. Refresh Token을 HttpOnly 쿠키에 저장
        refreshCookieManager.write(response, refreshToken);

        // 4. 마지막 접속 시간 업데이트
        memberService.updateLastLoginAt(member.getId());

        // 5. 사용자 상태에 따라 리다이렉트 결정 (Access Token만 URL로 전달)
        String redirectUrl = determineRedirectUrl(member, accessToken);

        log.info("OAuth2 login success for user: {}", member.getEmail());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String determineRedirectUrl(Member member, String accessToken) {
        String tokenParams = String.format("?access_token=%s", accessToken);
//        log.info("Generated access token: {}", accessToken);

        return frontendUrl + tokenParams;

//        if (!member.isVerified()) {
//            // 백준 인증 미완료 → 인증 페이지로
//            return baseUrl + "/auth/verify-boj" + tokenParams;
//        } else {
//            // 인증 완료 → 대시보드로
//            return baseUrl + "/dashboard" + tokenParams;
//        }
    }
}