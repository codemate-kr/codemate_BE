package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.MemberSolvedProblem;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import lombok.RequiredArgsConstructor;
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
    public Member getByHandle(String handle) {
        return memberRepository.findByHandle(handle)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<String> getVerifiedHandles() {
        return memberRepository.findAllVerifiedHandles();
    }

    public Member verifySolvedAcHandle(Long memberId, String handle) {
        // 1) solved.ac 에 존재하는지 확인 (예외 발생 시 검증 실패로 간주)
        solvedacService.getUserInfo(handle);

        // 2) 중복 핸들 체크
        Optional<Member> existing = memberRepository.findByHandle(handle);
        if (existing.isPresent() && !existing.get().getId().equals(memberId)) {
            throw new CustomException(CustomResponseStatus.HANDLE_ALREADY_EXISTS);
        }

        // 3) 사용자 엔티티에 핸들 저장 및 검증 완료 처리
        Member member = getById(memberId);
        member.verifyWithHandle(handle);
        return member;
    }
}
