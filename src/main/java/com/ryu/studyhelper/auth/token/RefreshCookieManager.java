package com.ryu.studyhelper.auth.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshCookieManager {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/auth/refresh";
    private static final boolean SECURE = true;
    private static final boolean HTTP_ONLY = true;
    private static final int MAX_AGE = -1; // 세션 쿠키(서버가 RT 만료 관리)

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void write(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, refreshToken);
        cookie.setPath(COOKIE_PATH);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setMaxAge(MAX_AGE);
        response.addCookie(cookie);
        // SameSite는 필요 시 Servlet 컨테이너/헤더로 추가 설정
    }

    public void clear(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath(COOKIE_PATH);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(SECURE);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}