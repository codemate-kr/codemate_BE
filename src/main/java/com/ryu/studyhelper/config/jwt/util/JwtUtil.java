package com.ryu.studyhelper.config.jwt.util;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    /** 액세스/리프레시 만료시간 */
    public static final long ACCESS_TOKEN_VALID_MS  = 1000L * 60 * 60;          // 1시간
//    public static final long ACCESS_TOKEN_VALID_MS  = 1000L * 30;          // 30초 테스트
    public static final long REFRESH_TOKEN_VALID_MS = 1000L * 60 * 60 * 24 * 14; // 14일
//    public static final long REFRESH_TOKEN_VALID_MS = 1000L * 60; // 1분 테스트

    private final Key secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long id, String role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(id));
        claims.put("role", role);
        claims.put("tokenType", "ACCESS");

        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALID_MS);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long id) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(id));
        claims.put("tokenType", "REFRESH");

        Date now = new Date();
        Date validity = new Date(now.getTime() + REFRESH_TOKEN_VALID_MS);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 권한 추출
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 토큰 유효성 검증
     */
    public void validateTokenOrThrow(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
            throw new CustomException(CustomResponseStatus.BAD_JWT);
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
            throw new CustomException(CustomResponseStatus.EXPIRED_JWT);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
            throw new CustomException(CustomResponseStatus.BAD_JWT);
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.");
            throw new CustomException(CustomResponseStatus.BAD_JWT);
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 토큰에서 Claims 추출
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * 토큰 타입 확인 (ACCESS or REFRESH)
     */
    public String getTokenType(String token) {
        return parseClaims(token).get("tokenType", String.class);
    }

    /**
     * Access Token 만료시간 반환 (밀리초)
     */
    public long getAccessTokenValidMs() {
        return ACCESS_TOKEN_VALID_MS;
    }

    /**
     * Refresh Token 만료시간 반환 (밀리초)
     */
    public long getRefreshTokenValidMs() {
        return REFRESH_TOKEN_VALID_MS;
    }
}