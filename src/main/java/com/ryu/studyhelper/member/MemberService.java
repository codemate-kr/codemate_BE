package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.security.jwt.JwtUtil;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.infrastructure.mail.dto.MailHtmlSendDto;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.MemberSolvedProblem;
import com.ryu.studyhelper.member.dto.response.DailySolvedResponse;
import com.ryu.studyhelper.member.dto.response.MemberSearchResponse;
import com.ryu.studyhelper.member.dto.response.MyProfileResponse;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.repository.MemberSolvedProblemRepository;
import com.ryu.studyhelper.problem.repository.ProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final MemberSolvedProblemRepository memberSolvedProblemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SolvedAcService solvedacService;
    private final JwtUtil jwtUtil;
    private final MailSendService mailSendService;
    private final Clock clock;

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public Member getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long memberId) {
        Member member = getById(memberId);
        long solvedCount = memberSolvedProblemRepository.countByMemberId(memberId);
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
        solvedacService.getUserInfo(handle);

        // 2. 회원 엔티티에 핸들 저장 (중복 허용, isVerified는 false 유지)
        Member member = getById(memberId);
        member.changeHandle(handle);

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
        // 1. 이메일 중복 확인
        if (!isEmailAvailable(newEmail)) {
            throw new CustomException(CustomResponseStatus.EMAIL_ALREADY_EXISTS);
        }

        // 2. 회원 존재 확인
        getById(memberId);

        // 3. 이메일 인증 토큰 생성 (5분 만료)
        String token = jwtUtil.createEmailVerificationToken(memberId, newEmail);

        // 4. 인증 URL 생성
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

        // 5. 이메일 발송 (전용 템플릿 사용)
        MailHtmlSendDto mailDto = MailHtmlSendDto.builder()
                .emailAddr(newEmail)
                .subject("[CodeMate] 이메일 변경 인증")
                .content("아래 버튼을 클릭하여 이메일 변경을 완료해주세요. 링크는 5분간 유효합니다.")
                .target("user")
                .buttonUrl(verificationUrl)
                .buttonText("이메일 인증하기")
                .templateName("email-change-template")
                .build();

        mailSendService.sendHtmlEmail(mailDto);
    }

    /**
     * 이메일 변경 인증 완료
     * @param token 이메일 인증 토큰
     * @return 변경된 이메일 주소
     */
    public String verifyAndChangeEmail(String token) {
        // 1. 토큰 유효성 검증 (만료, 잘못된 서명 등)
        jwtUtil.validateTokenOrThrow(token);

        // 2. 토큰 타입 확인
        String tokenType = jwtUtil.getTokenType(token);
        if (!JwtUtil.TOKEN_TYPE_EMAIL_VERIFICATION.equals(tokenType)) {
            throw new CustomException(CustomResponseStatus.INVALID_EMAIL_VERIFICATION_TOKEN);
        }

        // 3. 토큰에서 정보 추출
        Long memberId = jwtUtil.getIdFromToken(token);
        String newEmail = jwtUtil.getEmailFromToken(token);

        // 4. 이메일 중복 재확인 (인증 메일 발송 후 다른 사용자가 해당 이메일로 가입했을 수 있음)
        if (!isEmailAvailable(newEmail)) {
            throw new CustomException(CustomResponseStatus.EMAIL_ALREADY_EXISTS);
        }

        // 5. 회원 정보 업데이트
        Member member = getById(memberId);
        member.changeEmail(newEmail);

        return newEmail;
    }

    /**
     * 문제 해결 인증
     * @param memberId 회원 ID
     * @param problemId BOJ 문제 번호
     */
    public void verifyProblemSolved(Long memberId, Long problemId) {
        // 1. Member 조회
        Member member = getById(memberId);

        // 2. 핸들 존재 여부 확인 (조기 검증)
        if (member.getHandle() == null || member.getHandle().isEmpty()) {
            throw new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        }

        // 3. Problem 조회 (DB에 존재하는지 확인)
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.PROBLEM_NOT_FOUND));

        // 4. 이미 인증된 문제인지 확인
        if (memberSolvedProblemRepository.existsByMemberIdAndProblemId(memberId, problemId)) {
            throw new CustomException(CustomResponseStatus.ALREADY_SOLVED);
        }

        // 5. solved.ac API로 실제 해결 여부 검증
        boolean isSolved = solvedacService.hasUserSolvedProblem(member.getHandle(), problemId);

        if (!isSolved) {
            throw new CustomException(CustomResponseStatus.PROBLEM_NOT_SOLVED_YET);
        }

        // 6. MemberSolvedProblem 레코드 생성
        MemberSolvedProblem memberSolvedProblem = MemberSolvedProblem.create(member, problem);
        memberSolvedProblemRepository.save(memberSolvedProblem);
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

    private static final int MAX_DAILY_SOLVED_DAYS = 730;

    /**
     * 최근 N일간 일별 문제 풀이 현황 조회
     * - 날짜 기준: 오전 6시 (06:00 ~ 다음날 05:59를 하루로 계산)
     * @param memberId 회원 ID
     * @param days 조회할 일수 (1~730일)
     * @return 일별 풀이 현황
     */
    @Transactional(readOnly = true)
    public DailySolvedResponse getDailySolved(Long memberId, int days) {
        if (days < 1 || days > MAX_DAILY_SOLVED_DAYS) {
            throw new CustomException(CustomResponseStatus.INVALID_DAYS_RANGE);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = getAdjustedDate(now);

        // 조회 범위: (days-1)일 전 06:00 <= solvedAt < 내일 06:00
        LocalDateTime startDateTime = today.minusDays(days - 1).atTime(LocalTime.of(6, 0));
        LocalDateTime endDateTime = today.plusDays(1).atTime(LocalTime.of(6, 0));

        List<MemberSolvedProblem> solvedProblems = memberSolvedProblemRepository
                .findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(memberId, startDateTime, endDateTime);

        // 날짜별로 그룹핑 (오전 6시 기준)
        Map<LocalDate, List<DailySolvedResponse.SolvedProblem>> groupedByDate = new LinkedHashMap<>();

        // 먼저 모든 날짜를 빈 리스트로 초기화 (과거 → 현재 순서)
        for (int i = days - 1; i >= 0; i--) {
            groupedByDate.put(today.minusDays(i), new ArrayList<>());
        }

        // 풀이 데이터를 날짜별로 분류
        for (MemberSolvedProblem solved : solvedProblems) {
            LocalDate adjustedDate = getAdjustedDate(solved.getSolvedAt());
            Problem problem = solved.getProblem();

            DailySolvedResponse.SolvedProblem solvedProblem = new DailySolvedResponse.SolvedProblem(
                    problem.getId(),
                    problem.getTitleKo() != null ? problem.getTitleKo() : problem.getTitle(),
                    problem.getLevel()
            );

            if (groupedByDate.containsKey(adjustedDate)) {
                groupedByDate.get(adjustedDate).add(solvedProblem);
            }
        }

        // 응답 생성
        List<DailySolvedResponse.DailySolved> dailySolvedList = groupedByDate.entrySet().stream()
                .map(entry -> new DailySolvedResponse.DailySolved(
                        entry.getKey().toString(),
                        entry.getValue().size(),
                        entry.getValue()
                ))
                .toList();

        int totalCount = dailySolvedList.stream()
                .mapToInt(DailySolvedResponse.DailySolved::count)
                .sum();

        return new DailySolvedResponse(dailySolvedList, totalCount);
    }

    /**
     * 오전 6시 기준으로 날짜 계산
     * - 06:00 이전이면 전날로 처리
     */
    private LocalDate getAdjustedDate(LocalDateTime dateTime) {
        if (dateTime.getHour() < 6) {
            return dateTime.toLocalDate().minusDays(1);
        }
        return dateTime.toLocalDate();
    }

}

