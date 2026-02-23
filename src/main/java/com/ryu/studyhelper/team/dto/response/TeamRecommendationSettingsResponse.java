package com.ryu.studyhelper.team.dto.response;

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
        Integer maxProblemLevel, // 커스텀 모드일 때만 값 있음
        Integer problemCount, // 추천 문제 개수 (1~10, 기본값 3)
        List<String> includeTags, // 포함할 알고리즘 태그 키 목록
        String deprecationMessage
) {
    /**
     * 태그 정보 없이 응답 생성 (기존 API 호환용)
     */
    public static TeamRecommendationSettingsResponse from(Team team) {
        return from(team, List.of());
    }

    /**
     * 태그 정보 포함하여 응답 생성
     */
    public static TeamRecommendationSettingsResponse from(Team team, List<String> includeTags) {
        List<RecommendationDayOfWeek> days = team.getRecommendationDaysList();

        return new TeamRecommendationSettingsResponse(
                team.getId(),
                team.getName(),
                team.isRecommendationActive(),
                days,
                team.getProblemDifficultyPreset(),
                team.getEffectiveMinProblemLevel(),
                team.getEffectiveMaxProblemLevel(),
                team.getProblemCount(),
                includeTags != null ? includeTags : List.of(),
                "이 API는 deprecated 예정입니다. Squad 추천 설정 API(/api/teams/{teamId}/squads/{squadId}/recommendation-settings)로 전환해주세요."
        );
    }
}
