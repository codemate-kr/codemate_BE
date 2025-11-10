package com.ryu.studyhelper.recommendation.dto;

import com.ryu.studyhelper.recommendation.domain.team.TeamRecommendation;
import com.ryu.studyhelper.recommendation.domain.team.RecommendationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일일 추천 현황 요약 DTO
 * 사용자가 속한 팀들의 오늘 추천 현황을 보여줌
 */
public record DailyRecommendationSummary(
        Long teamId,
        String teamName,
        Long recommendationId,
        RecommendationStatus status,
        LocalDate recommendationDate,
        LocalDateTime sentAt,
        int problemCount
) {
    public static DailyRecommendationSummary from(TeamRecommendation recommendation) {
        return new DailyRecommendationSummary(
                recommendation.getTeam().getId(),
                recommendation.getTeam().getName(),
                recommendation.getId(),
                recommendation.getStatus(),
                recommendation.getRecommendationDate(),
                recommendation.getSentAt(),
                recommendation.getProblems().size()
        );
    }
}