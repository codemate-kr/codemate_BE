package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.discord.DiscordMessage;
import com.ryu.studyhelper.infrastructure.discord.DiscordNotifier;
import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.infrastructure.mail.sender.MailSender;
import com.ryu.studyhelper.member.mail.EmailChangeMailBuilder;
import com.ryu.studyhelper.member.mail.EmailVerificationClaim;
import com.ryu.studyhelper.member.mail.EmailVerificationTokenProvider;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.dto.response.MemberSearchResponse;
import com.ryu.studyhelper.member.dto.response.MyProfileResponse;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.solve.service.SolveService;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SolvedAcClient solvedAcClient;
    private final SolveService solveService;
    private final MailSender mailSender;
    private final EmailChangeMailBuilder emailChangeMailBuilder;
    private final EmailVerificationTokenProvider emailVerificationTokenProvider;
    private final DiscordNotifier discordNotifier;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Member getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long memberId) {
        Member member = getById(memberId);
        long solvedCount = solveService.countByMemberId(memberId);
        return MyProfileResponse.from(member, solvedCount);
    }

    @Transactional(readOnly = true)
    public List<MemberSearchResponse> searchByHandle(String handle) {
        List<Member> members = memberRepository.findAllByHandle(handle);
        return members.stream()
                .map(MemberSearchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getVerifiedHandles() {
        return memberRepository.findAllVerifiedHandles();
    }

    /**
     * 백준 핸들 등록 (solved.ac 존재 여부만 확인)
     * @param memberId 회원 ID
     * @param handle 백준 핸들
     * @return 업데이트된 회원 정보
     */
    public Member verifySolvedAcHandle(Long memberId, String handle) {
        // 1. solved.ac에 존재하는지 확인 (예외 발생 시 검증 실패)
        solvedAcClient.getUserInfo(handle);

        // 2. 회원 엔티티에 핸들 저장 (중복 허용, isVerified는 false 유지)
        Member member = getById(memberId);
        member.changeHandle(handle);

        try {
            discordNotifier.sendEvent(DiscordMessage.event("핸들 등록",
                    "회원 ID", String.valueOf(memberId),
                    "핸들", handle
            ));
        } catch (Exception e) {
            log.warn("Discord 알림 전송 실패", e);
        }

        return member;
    }

    /**
     * 이메일 중복 검사
     * @param email 검사할 이메일
     * @return 중복 여부 (true: 사용 가능, false: 이미 사용 중)
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return memberRepository.findByEmail(email).isEmpty();
    }

    /**
     * 이메일 변경 인증 메일 발송
     * @param memberId 회원 ID
     * @param newEmail 변경할 이메일
     */
    public void sendEmailVerification(Long memberId, String newEmail) {
        // 0. 이메일 유효성 확인
        if (newEmail == null || newEmail.isBlank()) {
            throw new CustomException(CustomResponseStatus.INVALID_EMAIL);
        }

        // 1. 이메일 중복 확인
        if (!isEmailAvailable(newEmail)) {
            throw new CustomException(CustomResponseStatus.EMAIL_ALREADY_EXISTS);
        }

        // 2. 회원 존재 확인
        getById(memberId);

        // 3. 인증 메일 생성 및 발송
        mailSender.send(emailChangeMailBuilder.build(memberId, newEmail));
    }

    /**
     * 이메일 변경 인증 완료
     * @param token 이메일 인증 토큰
     * @return 변경된 이메일 주소
     */
    public String verifyAndChangeEmail(String token) {
        // 1. 토큰 검증 및 클레임 추출
        EmailVerificationClaim claim = emailVerificationTokenProvider.parseToken(token);

        // 2. 이메일 중복 재확인 (인증 메일 발송 후 다른 사용자가 해당 이메일로 가입했을 수 있음)
        if (!isEmailAvailable(claim.newEmail())) {
            throw new CustomException(CustomResponseStatus.EMAIL_ALREADY_EXISTS);
        }

        // 3. 회원 정보 업데이트
        Member member = getById(claim.memberId());
        member.changeEmail(claim.newEmail());

        return claim.newEmail();
    }

    /**
     * 마지막 접속 시간 업데이트
     * @param memberId 회원 ID
     */
    public void updateLastLoginAt(Long memberId) {
        try {
            Member member = getById(memberId);
            member.updateLastLoginAt(LocalDateTime.now(clock));
        } catch (Exception e) {
            log.warn("마지막 접속 시간 업데이트 실패: memberId={}", memberId, e);
        }
    }

    /**
     * 회원 탈퇴
     * - 모든 팀에서 탈퇴한 상태여야 함
     * - 민감정보(이메일, 핸들, providerId) 마스킹 후 소프트 딜리트
     * @param memberId 회원 ID
     */
    public void withdraw(Long memberId) {
        Member member = getById(memberId);

        // 팀 소속 여부 확인
        boolean hasTeam = teamMemberRepository.existsByMemberId(memberId);
        if (hasTeam) {
            throw new CustomException(CustomResponseStatus.MEMBER_HAS_TEAM);
        }

        // 민감정보 마스킹 + 소프트 딜리트
        member.withdraw();
        memberRepository.save(member);
    }

}
