package com.ryu.studyhelper.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /***
     * JWT 인증이 실패했을 때 호출되는 메서드
     * 토큰이 없거나, 유효하지 않은 경우 401 응답 반환
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        
        String exception = (String) request.getAttribute("exception");
        
        log.error("JWT Authentication failed: {} | Exception: {}", authException.getMessage(), exception);
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 401);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getRequestURI());
        
        // 예외 유형에 따른 세부 메시지 설정
        if (exception != null) {
            switch (exception) {
                case "EXPIRED_TOKEN":
                    errorResponse.put("error", "EXPIRED_TOKEN");
                    errorResponse.put("message", "토큰이 만료되었습니다. 새로운 토큰으로 다시 시도해주세요.");
                    break;
                case "MALFORMED_TOKEN":
                    errorResponse.put("error", "MALFORMED_TOKEN");
                    errorResponse.put("message", "토큰 형식이 올바르지 않습니다.");
                    break;
                case "UNSUPPORTED_TOKEN":
                    errorResponse.put("error", "UNSUPPORTED_TOKEN");
                    errorResponse.put("message", "지원되지 않는 토큰입니다.");
                    break;
                case "INVALID_SIGNATURE":
                    errorResponse.put("error", "INVALID_SIGNATURE");
                    errorResponse.put("message", "토큰 서명이 유효하지 않습니다.");
                    break;
                case "EMPTY_TOKEN":
                    errorResponse.put("error", "EMPTY_TOKEN");
                    errorResponse.put("message", "토큰이 비어있습니다.");
                    break;
                default:
                    errorResponse.put("error", "AUTHENTICATION_FAILED");
                    errorResponse.put("message", "인증에 실패했습니다.");
            }
        } else {
            errorResponse.put("error", "UNAUTHORIZED");
            errorResponse.put("message", "인증이 필요합니다. 유효한 토큰을 제공해주세요.");
        }
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
