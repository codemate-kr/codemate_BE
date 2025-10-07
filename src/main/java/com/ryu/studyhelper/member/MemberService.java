package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.jwt.util.JwtUtil;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.infrastructure.mail.dto.MailHtmlSendDto;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.MemberSolvedProblem;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final MemberSolvedProblemRepository memberSolvedProblemRepository;
    private final SolvedAcService solvedacService;
    private final JwtUtil jwtUtil;
    private final MailSendService mailSendService;

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

//    public void syncSolvedProblems(String handle) {
//        // 1. handle 기반 Member 조회
//        Member member = memberRepository.findByHandle(handle)
//                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));
//
//        // 2. solved.ac 에서 해당 회원이 푼 문제 목록 가져오기
//        List<ProblemInfo> solvedProblems = solvedacService.fetchSolvedProblems(handle);
//
//        for (ProblemInfo info : solvedProblems) {
//            // 3. 문제 DB에 저장 (기존에 없으면 insert, 있으면 skip)
//            Problem problem = problemRepository.findById(info.getProblemId())
//                    .orElseGet(() -> problemRepository.save(Problem.from(info)));
//
//            // 4. 이미 저장된 관계가 없다면 MemberSolvedProblem 저장
//            if (!memberSolvedProblemRepository.existsByMemberAndProblem(member, problem)) {
//                memberSolvedProblemRepository.save(MemberSolvedProblem.builder()
//                        .member(member)
//                        .problem(problem)
//                        .solvedAt(LocalDateTime.now()) // solved.ac가 시간 제공 안 하면 now() 사용
//                        .build());
//            }
//        }
//    }

    @Transactional(readOnly = true)
    public Member getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Member> getAllByHandle(String handle) {
        List<Member> members = memberRepository.findAllByHandle(handle);
        if (members.isEmpty()) {
            throw new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND);
        }
        return members;
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
        return !memberRepository.findByEmail(email).isPresent();
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
        Member member = getById(memberId);

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
     * @return 업데이트된 회원 정보
     */
    public Member verifyAndChangeEmail(String token) {
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

        return member;
    }

}
