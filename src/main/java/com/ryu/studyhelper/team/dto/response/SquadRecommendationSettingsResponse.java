package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.ProblemDifficultyPreset;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.Squad;

import java.util.List;

public record SquadRecommendationSettingsResponse(
        Long squadId,
        String squadName,
        boolean isActive,
        List<RecommendationDayOfWeek> recommendationDays,
        ProblemDifficultyPreset problemDifficultyPreset,
        Integer minProblemLevel,
        Integer maxProblemLevel,
        Integer problemCount,
        List<String> includeTags
) {
    public static SquadRecommendationSettingsResponse from(Squad squad, List<String> includeTags) {
        return new SquadRecommendationSettingsResponse(
                squad.getId(),
                squad.getName(),
                squad.isRecommendationActive(),
                squad.getRecommendationDaysList(),
                squad.getProblemDifficultyPreset(),
                squad.getEffectiveMinProblemLevel(),
                squad.getEffectiveMaxProblemLevel(),
                squad.getProblemCount(),
                includeTags != null ? includeTags : List.of()
        );
    }
}
