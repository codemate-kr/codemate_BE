package com.ryu.studyhelper.solve.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.solve.dto.response.DailySolvedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SolveFacade {

    private final SolveService solveService;
    private final SolvedAcClient solvedAcClient;

    /**
     * 문제 해결 인증
     * SolvedAC 호출 구간에서 DB 커넥션을 점유하지 않도록 트랜잭션을 분리
     */
    public void verifyProblemSolved(Long memberId, Long problemId) {
        String handle = solveService.validateAndGetHandle(memberId, problemId); // 트랜잭션 종료 → 커넥션 반환
        boolean isSolved = solvedAcClient.hasUserSolvedProblem(handle, problemId); // 커넥션 없음
        if (!isSolved) {
            throw new CustomException(CustomResponseStatus.PROBLEM_NOT_SOLVED_YET);
        }
        solveService.recordSolved(memberId, problemId); // 새 트랜잭션 → 커넥션 재획득
    }

    public DailySolvedResponse getDailySolved(Long memberId, int days) {
        return solveService.getDailySolved(memberId, days);
    }
}
