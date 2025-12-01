package com.ryu.studyhelper.team.dto.request;

import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.ProblemDifficultyPreset;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;

/**
 * 팀별 추천 설정 요청 DTO
 */
@Schema(description = "팀별 문제 추천 설정 요청")
public record TeamRecommendationSettingsRequest(

//        @NotEmpty(message = "추천받을 요일을 최소 1개 이상 선택해야 합니다.")
        @Schema(description = "추천받을 요일들 (월요일부터 일요일까지 순서대로)",
                example = "[\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\", \"THURSDAY\", \"FRIDAY\"]")
        List<RecommendationDayOfWeek> recommendationDays,

        @NotNull(message = "난이도 프리셋을 선택해야 합니다.")
        @Schema(description = "문제 난이도 프리셋",
                example = "NORMAL",
                allowableValues = {"EASY", "NORMAL", "HARD", "CUSTOM"})
        ProblemDifficultyPreset problemDifficultyPreset,

        @Min(value = 1, message = "최소 난이도는 1 이상이어야 합니다.")
        @Max(value = 30, message = "최소 난이도는 30 이하여야 합니다.")
        @Schema(description = "커스텀 모드일 때 최소 난이도 (1~30)", example = "1")
        Integer minProblemLevel,

        @Min(value = 1, message = "최대 난이도는 1 이상이어야 합니다.")
        @Max(value = 30, message = "최대 난이도는 30 이하여야 합니다.")
        @Schema(description = "커스텀 모드일 때 최대 난이도 (1~30)", example = "30")
        Integer maxProblemLevel,

        @Min(value = 1, message = "추천 문제 개수는 1 이상이어야 합니다.")
        @Max(value = 10, message = "추천 문제 개수는 10 이하여야 합니다.")
        @Schema(description = "추천 문제 개수 (1~10, 기본값 3)", example = "3")
        Integer problemCount

) {}