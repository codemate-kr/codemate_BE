package com.ryu.studyhelper.team.dto;

import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.ProblemDifficultyPreset;
import com.ryu.studyhelper.team.domain.Team;

import java.util.Set;

/**
 * 팀별 추천 설정 응답 DTO
 */
public record TeamRecommendationSettingsResponse(
        Long teamId,
        String teamName,
        boolean isActive,
        Set<RecommendationDayOfWeek> recommendationDays,
        String[] recommendationDayNames, // 한글 요일명
        ProblemDifficultyPreset problemDifficultyPreset,
        String difficultyDisplayName, // 난이도 프리셋 한글명
        Integer customMinLevel, // 커스텀 모드일 때만 값 있음
        Integer customMaxLevel // 커스텀 모드일 때만 값 있음
) {
    public static TeamRecommendationSettingsResponse from(Team team) {
        Set<RecommendationDayOfWeek> days = team.getRecommendationDaysSet();
        String[] dayNames = days.stream()
                .map(RecommendationDayOfWeek::getKoreanName)
                .toArray(String[]::new);

        return new TeamRecommendationSettingsResponse(
                team.getId(),
                team.getName(),
                team.isRecommendationActive(),
                days,
                dayNames,
                team.getProblemDifficultyPreset(),
                team.getProblemDifficultyPreset().getDisplayName(),
                team.getMinProblemLevel(), // 커스텀일 때만 값 있음
                team.getMaxProblemLevel() // 커스텀일 때만 값 있음
        );
    }
}