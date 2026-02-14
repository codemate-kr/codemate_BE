package com.ryu.studyhelper.recommendation.dto.response;

import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.team.domain.Team;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.ryu.studyhelper.problem.domain.Problem;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 추천 상세 조회용 응답 DTO
 */
public record RecommendationDetailResponse(
        Long id,
        String teamName,
        LocalDate recommendationDate,
        RecommendationType type,
        String status,
        LocalDateTime sentAt,
        List<RecommendedProblemDetail> problems
) {
    /**
     * 추천된 문제 상세 정보
     */
    public record RecommendedProblemDetail(
            Long problemId,
            String title,
            String titleKo,
            Integer level,
            String url,
            Integer recommendationOrder,
            Integer acceptedUserCount,
            Double averageTries
    ) {}

    public static RecommendationDetailResponse from(Recommendation recommendation, Team team, List<Problem> problems) {
        return new RecommendationDetailResponse(
                recommendation.getId(),
                team.getName(),
                recommendation.getCreatedAt().toLocalDate(),
                recommendation.getType(),
                "SENT",
                LocalDateTime.now(),
                IntStream.range(0, problems.size())
                        .mapToObj(i -> {
                            Problem p = problems.get(i);
                            return new RecommendedProblemDetail(
                                    p.getId(),
                                    p.getTitle(),
                                    p.getTitleKo(),
                                    p.getLevel(),
                                    p.getUrl(),
                                    i + 1,
                                    p.getAcceptedUserCount(),
                                    p.getAverageTries()
                            );
                        })
                        .toList()
        );
    }
}
