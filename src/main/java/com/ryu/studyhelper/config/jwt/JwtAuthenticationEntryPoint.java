package com.ryu.studyhelper.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 인증 실패(컨트롤러 도달 전) 시 호출되어 통일된 에러 포맷(ApiResponse)으로 응답한다.
     * 필터에서 request.setAttribute("exception", "...")로 설정한 태그 값을 참조한다.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        // JwtAuthenticationFilter 에서 setAttribute("exception", TAG) 로 심어둔 값
        final String tag = (String) request.getAttribute("exception");
        log.debug("Auth failure tag from filter: {}", tag);

        // 태그 → CustomResponseStatus 매핑 (UNKNOWN_ERROR도 401로, 태그 없는 경우만 403)
        final String tagSafe = (tag == null) ? "" : tag;
        final CustomResponseStatus status = switch (tagSafe) {
            case "EXPIRED_TOKEN"     -> CustomResponseStatus.EXPIRED_JWT; // 401
            case "MALFORMED_TOKEN"   -> CustomResponseStatus.BAD_JWT;     // 401
            case "UNSUPPORTED_TOKEN" -> CustomResponseStatus.BAD_JWT;     // 401
            case "INVALID_SIGNATURE" -> CustomResponseStatus.BAD_JWT;     // 401
            case "EMPTY_TOKEN"       -> CustomResponseStatus.BAD_JWT;     // 401
            case "BAD_TOKEN"         -> CustomResponseStatus.BAD_TOKEN;   // 400
            case "UNKNOWN_ERROR"     -> CustomResponseStatus.BAD_JWT;     // 401 (필터에서 일반 예외로 태깅된 경우)
            case ""                  -> CustomResponseStatus.ACCESS_DENIED; // 403 (태그 없음 = 토큰 미제공 등)
            default                  -> CustomResponseStatus.BAD_JWT;     // 401 (알 수 없는 태그는 401로 처리)
        };

        log.error("JWT Authentication failed: {} | tag: {}", authException.getMessage(), tag);

        response.setStatus(status.getHttpStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        // 통일 포맷으로 응답 ({"httpStatusCode", "code", "message", "data": null})
        String body = objectMapper.writeValueAsString(ApiResponse.createError(status));
        response.getWriter().write(body);
    }
}
