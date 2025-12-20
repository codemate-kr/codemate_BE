package com.ryu.studyhelper.team.dto.internal;

import java.util.Set;

/**
 * 멤버의 문제 풀이 상태를 나타내는 내부 DTO
 * TeamActivityService 내부에서만 사용됩니다.
 */
public record MemberSolvedStatus(
        Long memberId,
        Set<Long> solvedProblemIds
) {
    public boolean hasSolved(Long problemId) {
        return solvedProblemIds.contains(problemId);
    }
}