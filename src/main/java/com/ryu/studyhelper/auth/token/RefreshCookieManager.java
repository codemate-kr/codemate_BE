package com.ryu.studyhelper.auth.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

import static com.ryu.studyhelper.config.security.jwt.JwtUtil.REFRESH_TOKEN_VALID_MS;

@Component
public class RefreshCookieManager {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth/refresh";
    private static final boolean SECURE = true;
    private static final boolean HTTP_ONLY = true;
    private static final long MAX_AGE = REFRESH_TOKEN_VALID_MS / 1000; // 세션 쿠키(서버가 RT 만료 관리)

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void write(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, refreshToken)
                .path(COOKIE_PATH)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .sameSite("None")
                .maxAge(MAX_AGE)
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .path(COOKIE_PATH)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .sameSite("None")
                .maxAge(0)
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
    }
}