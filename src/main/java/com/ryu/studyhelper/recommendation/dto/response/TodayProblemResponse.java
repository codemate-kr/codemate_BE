package com.ryu.studyhelper.recommendation.dto.response;

import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오늘의 문제 조회용 응답 DTO (해결 여부 포함)
 */
public record TodayProblemResponse(
        Long recommendationId,
        LocalDateTime createdAt,
        List<ProblemWithSolvedStatus> problems
) {
    public record ProblemWithSolvedStatus(
            Long problemId,
            String title,
            String titleKo,
            Integer level,
            String url,
            Integer acceptedUserCount,
            Double averageTries,
            Boolean isSolved
    ) {
        public static ProblemWithSolvedStatus from(Problem problem, boolean isSolved) {
            return new ProblemWithSolvedStatus(
                    problem.getId(),
                    problem.getTitle(),
                    problem.getTitleKo(),
                    problem.getLevel(),
                    problem.getUrl(),
                    problem.getAcceptedUserCount(),
                    problem.getAverageTries(),
                    isSolved
            );
        }
    }

    public static TodayProblemResponse from(Recommendation recommendation, List<Long> solvedProblemIds) {
        List<ProblemWithSolvedStatus> problemsWithStatus = recommendation.getProblems().stream()
                .map(rp -> ProblemWithSolvedStatus.from(
                        rp.getProblem(),
                        solvedProblemIds.contains(rp.getProblem().getId())
                ))
                .toList();

        return new TodayProblemResponse(
                recommendation.getId(),
                recommendation.getCreatedAt(),
                problemsWithStatus
        );
    }
}