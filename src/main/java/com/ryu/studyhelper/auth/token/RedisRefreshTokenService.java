package com.ryu.studyhelper.auth.token;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.security.jwt.JwtUtil;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenService implements RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @Override
    public void saveRefreshToken(Long id, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + id;
        long ttlInSeconds = JwtUtil.REFRESH_TOKEN_VALID_MS / 1000;

        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(ttlInSeconds));
        log.debug("Refresh token saved for user: {}", id);
    }

    @Override
    public String getRefreshToken(Long id) {
        String key = REFRESH_TOKEN_PREFIX + id;
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteRefreshToken(Long id) {
        String key = REFRESH_TOKEN_PREFIX + id;
        redisTemplate.delete(key);
        log.debug("Refresh token deleted for user: {}", id);
    }

    @Override
    public boolean existsRefreshToken(Long id) {
        String key = REFRESH_TOKEN_PREFIX + id;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public String refreshAccessToken(String refreshToken) throws RefreshTokenException {
        // 1. Refresh Token 검증
        jwtUtil.validateTokenOrThrow(refreshToken);

        // 1-1. 토큰 타입 확인 (REFRESH만 허용)
        if (!"REFRESH".equalsIgnoreCase(jwtUtil.getTokenType(refreshToken))) {
            throw new CustomException(CustomResponseStatus.REFRESH_TOKEN_NOT_FOUND);
        }

        // 2. 토큰에서 id 추출
        Long id = jwtUtil.getIdFromToken(refreshToken);

        //TODO: 다중기기 로그인 구현

        // 3. Redis에서 저장된 Refresh Token과 비교
        validateRefreshToken(id, refreshToken);

        // 4. 사용자 정보 조회
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RefreshTokenException("User not found: " + id));

        // 5. 새로운 Access Token 생성
        return jwtUtil.createAccessToken(member.getId(), member.getRole().name());
    }

    @Override
    public void validateRefreshToken(Long id, String refreshToken) throws RefreshTokenException {
        String storedToken = getRefreshToken(id);
        
        if (storedToken == null) {
            throw new CustomException(CustomResponseStatus.REFRESH_TOKEN_NOT_FOUND);
        }
        
        if (!storedToken.equals(refreshToken)) {
            // 토큰 재사용 의심 - 저장된 토큰 삭제
            deleteRefreshToken(id);
            throw new CustomException(CustomResponseStatus.REFRESH_TOKEN_REUSE_DETECTED);
        }
    }
}