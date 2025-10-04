package com.ryu.studyhelper.member.verification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 백준 핸들 인증을 위한 해시 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BojVerificationService {

    private static final String VERIFICATION_KEY_PREFIX = "member:verification:";
    private static final long VERIFICATION_TTL_MINUTES = 10;
    private static final String HASH_PREFIX = "CodeMate-";
    private static final int HASH_LENGTH = 8;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 인증용 해시 생성 및 Redis에 저장
     * @param memberId 회원 ID
     * @return 생성된 해시값 (CodeMate-xxxxxxxx)
     */
    public String generateVerificationHash(Long memberId) {
        String hash = HASH_PREFIX + generateRandomString(HASH_LENGTH);
        String key = VERIFICATION_KEY_PREFIX + memberId;

        redisTemplate.opsForValue().set(key, hash, VERIFICATION_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("Generated verification hash for member {}: {}", memberId, hash);

        return hash;
    }

    /**
     * 저장된 인증 해시 조회
     * @param memberId 회원 ID
     * @return 저장된 해시값 (없으면 Optional.empty())
     */
    public Optional<String> getVerificationHash(Long memberId) {
        String key = VERIFICATION_KEY_PREFIX + memberId;
        String hash = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(hash);
    }

    /**
     * 인증 해시 삭제
     * @param memberId 회원 ID
     */
    public void deleteVerificationHash(Long memberId) {
        String key = VERIFICATION_KEY_PREFIX + memberId;
        redisTemplate.delete(key);
        log.info("Deleted verification hash for member {}", memberId);
    }

    /**
     * 랜덤 문자열 생성 (영숫자)
     */
    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }
}