package com.ryu.studyhelper.recommendation.dto;

import com.ryu.studyhelper.recommendation.domain.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 팀별 추천 이력 목록 조회용 응답 DTO
 */
public record TeamRecommendationHistoryResponse(
        Long id,
        LocalDate recommendationDate,
        RecommendationType type,
        RecommendationStatus status,
        LocalDateTime sentAt,
        int problemCount,
        List<String> problemTitles
) {
    public static TeamRecommendationHistoryResponse from(TeamRecommendation recommendation) {
        return new TeamRecommendationHistoryResponse(
                recommendation.getId(),
                recommendation.getRecommendationDate(),
                recommendation.getType(),
                recommendation.getStatus(),
                recommendation.getSentAt(),
                recommendation.getProblems().size(),
                recommendation.getProblems().stream()
                        .map(p -> p.getProblem().getTitleKo())
                        .limit(3) // 최대 3개만 표시
                        .toList()
        );
    }
}