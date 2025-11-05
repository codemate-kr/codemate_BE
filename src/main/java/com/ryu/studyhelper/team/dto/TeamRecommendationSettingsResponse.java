package com.ryu.studyhelper.team.dto;

import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.ProblemDifficultyPreset;
import com.ryu.studyhelper.team.domain.Team;

import java.util.List;

/**
 * 팀별 추천 설정 응답 DTO
 */
public record TeamRecommendationSettingsResponse(
        Long teamId,
        String teamName,
        boolean isActive,
        List<RecommendationDayOfWeek> recommendationDays, // 월요일부터 일요일까지 순서대로
        ProblemDifficultyPreset problemDifficultyPreset,
        Integer minProblemLevel, // 커스텀 모드일 때만 값 있음
        Integer maxProblemLevel // 커스텀 모드일 때만 값 있음
) {
    public static TeamRecommendationSettingsResponse from(Team team) {
        List<RecommendationDayOfWeek> days = team.getRecommendationDaysList();

        return new TeamRecommendationSettingsResponse(
                team.getId(),
                team.getName(),
                team.isRecommendationActive(),
                days,
                team.getProblemDifficultyPreset(),
                team.getEffectiveMinProblemLevel(),
                team.getEffectiveMaxProblemLevel()
        );
    }
}