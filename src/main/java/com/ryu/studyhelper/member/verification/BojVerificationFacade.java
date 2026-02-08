package com.ryu.studyhelper.member.verification;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.verification.dto.GenerateHashResponse;
import com.ryu.studyhelper.member.verification.dto.VerifyBojResponse;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserBioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 백준 핸들 인증 파사드 서비스
 * MemberService의 인증 관련 로직을 분리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BojVerificationFacade {

    private final MemberRepository memberRepository;
    private final SolvedAcClient solvedAcClient;
    private final BojVerificationService bojVerificationService;

    /**
     * 백준 핸들 인증용 해시 생성
     * @param memberId 회원 ID
     * @return 생성된 해시값
     */
    public GenerateHashResponse generateVerificationHash(Long memberId) {
        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        // 이미 인증된 회원인 경우
        if (member.isVerified()) {
            throw new CustomException(CustomResponseStatus.ALREADY_VERIFIED);
        }

        // 해시 생성 및 Redis에 저장
        String hash = bojVerificationService.generateVerificationHash(memberId);
        log.info("Generated BOJ verification hash for member {}: {}", memberId, hash);

        return GenerateHashResponse.of(hash);
    }

    /**
     * 백준 핸들 검증
     * @param memberId 회원 ID
     * @param handle 백준 핸들
     * @return 검증 결과
     */
    public VerifyBojResponse verifyBojHandle(Long memberId, String handle) {
        // 1. 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        // 2. 이미 인증된 회원인 경우
        if (member.isVerified()) {
            throw new CustomException(CustomResponseStatus.ALREADY_VERIFIED);
        }

        // 3. Redis에서 저장된 해시 조회
        String savedHash = bojVerificationService.getVerificationHash(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.VERIFICATION_HASH_NOT_FOUND));

        // 4. solved.ac API에서 사용자 bio 조회
        SolvedAcUserBioResponse userInfo;
        try {
            userInfo = solvedAcClient.getUserBio(handle);
        } catch (Exception e) {
            log.error("Failed to fetch user bio from solved.ac for handle: {}", handle, e);
            return VerifyBojResponse.failure("해당 백준 핸들을 찾을 수 없습니다.");
        }

        // 5. bio에 해시가 포함되어 있는지 확인
        if (userInfo.bio() == null || !userInfo.bio().contains(savedHash)) {
            log.warn("BOJ verification failed for member {}: bio does not contain hash", memberId);
            return VerifyBojResponse.failure("상태 메시지에 인증 해시가 없습니다. 다시 확인해주세요.");
        }

        // 6. 중복 핸들 체크 (중복 허용하므로 체크 제거)

        // 7. 인증 완료 처리
        member.verifyWithHandle(handle);

        // 8. Redis에서 해시 삭제
        bojVerificationService.deleteVerificationHash(memberId);

        log.info("BOJ verification successful for member {}: {}", memberId, handle);
        return VerifyBojResponse.success(handle);
    }
}