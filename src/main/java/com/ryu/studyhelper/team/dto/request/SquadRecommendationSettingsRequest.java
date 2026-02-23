package com.ryu.studyhelper.team.dto.request;

import com.ryu.studyhelper.team.domain.ProblemDifficultyPreset;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SquadRecommendationSettingsRequest(
        List<RecommendationDayOfWeek> recommendationDays,

        @NotNull(message = "난이도 프리셋을 선택해야 합니다.")
        ProblemDifficultyPreset problemDifficultyPreset,

        @Min(value = 1, message = "최소 난이도는 1 이상이어야 합니다.")
        @Max(value = 30, message = "최소 난이도는 30 이하여야 합니다.")
        Integer minProblemLevel,

        @Min(value = 1, message = "최대 난이도는 1 이상이어야 합니다.")
        @Max(value = 30, message = "최대 난이도는 30 이하여야 합니다.")
        Integer maxProblemLevel,

        @Min(value = 1, message = "추천 문제 개수는 1 이상이어야 합니다.")
        @Max(value = 10, message = "추천 문제 개수는 10 이하여야 합니다.")
        Integer problemCount,

        @Size(max = 10, message = "포함 태그는 최대 10개까지 설정 가능합니다.")
        List<String> includeTags
) {}
