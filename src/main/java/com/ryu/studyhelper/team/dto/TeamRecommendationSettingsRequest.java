package com.ryu.studyhelper.team.dto;

import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * 팀별 추천 설정 요청 DTO
 */
@Schema(description = "팀별 문제 추천 설정 요청")
public record TeamRecommendationSettingsRequest(
        
        @NotEmpty(message = "추천받을 요일을 최소 1개 이상 선택해야 합니다.")
        @Schema(description = "추천받을 요일들", 
                example = "[\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\", \"THURSDAY\", \"FRIDAY\"]")
        Set<RecommendationDayOfWeek> recommendationDays
        
) {}