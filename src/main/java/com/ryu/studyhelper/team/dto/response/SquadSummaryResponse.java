package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import com.ryu.studyhelper.team.domain.ProblemDifficultyPreset;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.Squad;

import java.util.List;

public record SquadSummaryResponse(
        Long squadId,
        String name,
        String description,
        boolean isDefault,
        int memberCount,
        boolean isActive,
        List<RecommendationDayOfWeek> recommendationDays,
        ProblemDifficultyPreset problemDifficultyPreset,
        Integer minProblemLevel,
        Integer maxProblemLevel,
        Integer problemCount,
        List<String> includeTags,
        TodayProblemResponse todayProblems
) {
    public static SquadSummaryResponse from(Squad squad, int memberCount, List<String> includeTags,
                                            TodayProblemResponse todayProblems) {
        return new SquadSummaryResponse(
                squad.getId(),
                squad.getName(),
                squad.getDescription(),
                squad.isDefault(),
                memberCount,
                squad.isRecommendationActive(),
                squad.getRecommendationDaysList(),
                squad.getProblemDifficultyPreset(),
                squad.getEffectiveMinProblemLevel(),
                squad.getEffectiveMaxProblemLevel(),
                squad.getProblemCount(),
                includeTags != null ? includeTags : List.of(),
                todayProblems
        );
    }
}
