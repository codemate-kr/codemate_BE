package com.ryu.studyhelper.recommendation.dto;

import com.ryu.studyhelper.recommendation.domain.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 팀 추천 상세 조회용 응답 DTO
 */
public record TeamRecommendationDetailResponse(
        Long id,
        String teamName,
        LocalDate recommendationDate,
        RecommendationType type,
        RecommendationStatus status,
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

    public static TeamRecommendationDetailResponse from(TeamRecommendation recommendation) {
        return new TeamRecommendationDetailResponse(
                recommendation.getId(),
                recommendation.getTeam().getName(),
                recommendation.getRecommendationDate(),
                recommendation.getType(),
                recommendation.getStatus(),
                recommendation.getSentAt(),
                recommendation.getProblems().stream()
                        .map(trp -> new RecommendedProblemDetail(
                                trp.getProblem().getId(),
                                trp.getProblem().getTitle(),
                                trp.getProblem().getTitleKo(),
                                trp.getProblem().getLevel(),
                                trp.getProblem().getUrl(),
                                trp.getRecommendationOrder(),
                                trp.getProblem().getAcceptedUserCount(),
                                trp.getProblem().getAverageTries()
                        ))
                        .sorted((a, b) -> Integer.compare(a.recommendationOrder(), b.recommendationOrder()))
                        .toList()
        );
    }
}
