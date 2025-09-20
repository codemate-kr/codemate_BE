package com.ryu.studyhelper.config.jwt.filter;

import com.ryu.studyhelper.config.jwt.util.JwtUtil;
import com.ryu.studyhelper.config.security.PrincipalDetailsService;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;


@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final PrincipalDetailsService principalDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. 요청 헤더에서 JWT 토큰 추출
            String token = resolveToken(request);

            // 2. 토큰 유효성 검증
            if (token != null) {
                //jwt 검증
                jwtUtil.validateTokenOrThrow(token);

                // 3. 토큰에서 사용자 정보 추출
                Long id = jwtUtil.getIdFromToken(token);

                // 4. 사용자 상세 정보 로드
                UserDetails userDetails = principalDetailsService.loadUserByUsername(String.valueOf(id));

                // 5. 인증 객체 생성 및 SecurityContext에 저장
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 인증 성공: {}", id);

            }
        } catch (CustomException e) {
            CustomResponseStatus s = e.getStatus();
            if (s == CustomResponseStatus.EXPIRED_JWT) {
                request.setAttribute("exception", "EXPIRED_TOKEN");
            } else if (s == CustomResponseStatus.BAD_JWT) {
                request.setAttribute("exception", "MALFORMED_TOKEN");
            } else if (s == CustomResponseStatus.BAD_TOKEN) {
                request.setAttribute("exception", "BAD_TOKEN");
            } else {
                request.setAttribute("exception", "UNKNOWN_ERROR");
            }
            log.error("JWT 도메인 예외 발생: {} ({})", s.getMessage(), s.getCode());
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 예상치 못한 오류가 발생했습니다: {}", e.getMessage());
            request.setAttribute("exception", "UNKNOWN_ERROR");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 JWT 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * 특정 경로는 JWT 검증 건너뛰기
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2")
                || path.startsWith("/login")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/public")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/error")
                || path.startsWith("/html")
                || path.startsWith("/css")
                || path.startsWith("/js");
    }
}