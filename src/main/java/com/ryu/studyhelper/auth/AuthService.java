package com.ryu.studyhelper.auth;

import com.ryu.studyhelper.auth.dto.AccessToken;
import com.ryu.studyhelper.auth.token.RefreshCookieManager;
import com.ryu.studyhelper.auth.token.RefreshTokenService;
import com.ryu.studyhelper.config.jwt.util.JwtUtil;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenService refreshTokenService;
    private final RefreshCookieManager refreshCookieManager;
    private final JwtUtil jwtUtil;

    public AccessToken refresh(HttpServletRequest request) {
        String refreshToken = refreshCookieManager.read(request)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.REFRESH_TOKEN_NOT_FOUND));

        String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);
        long expiresIn = jwtUtil.getAccessTokenValidMs() / 1000;
        
        return new AccessToken(newAccessToken, expiresIn);
    }


    public void logout(String accessToken, HttpServletResponse response) {
        jwtUtil.validateTokenOrThrow(accessToken);
        Long userId = jwtUtil.getIdFromToken(accessToken);

        refreshTokenService.deleteRefreshToken(userId);
        refreshCookieManager.clear(response);
    }
}