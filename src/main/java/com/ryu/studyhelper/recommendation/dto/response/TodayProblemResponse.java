package com.ryu.studyhelper.recommendation.dto.response;

import com.ryu.studyhelper.common.util.ProblemUrlUtils;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.dto.projection.ProblemWithSolvedStatusProjection;

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
        public static ProblemWithSolvedStatus from(ProblemWithSolvedStatusProjection projection) {
            return new ProblemWithSolvedStatus(
                    projection.getProblemId(),
                    projection.getTitle(),
                    projection.getTitleKo(),
                    projection.getLevel(),
                    ProblemUrlUtils.generateProblemUrl(projection.getProblemId()),
                    projection.getAcceptedUserCount(),
                    projection.getAverageTries(),
                    projection.getIsSolved()
            );
        }
    }

    public static TodayProblemResponse from(Recommendation recommendation, List<ProblemWithSolvedStatusProjection> projections) {
        List<ProblemWithSolvedStatus> problems = projections.stream()
                .map(ProblemWithSolvedStatus::from)
                .toList();

        return new TodayProblemResponse(recommendation.getId(), recommendation.getCreatedAt(), problems);
    }
}