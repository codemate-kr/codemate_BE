package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.Squad;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공개 팀 정보 응답 v2 (스쿼드 기반)")
public record PublicTeamResponseV2(
        @Schema(description = "팀 ID", example = "1")
        Long teamId,

        @Schema(description = "팀 이름", example = "알고리즘 스터디")
        String teamName,

        @Schema(description = "팀 설명", example = "매주 알고리즘 문제를 함께 풀어보는 스터디입니다.")
        String description,

        @Schema(description = "팀장 BOJ 핸들", example = "tourist")
        String leaderHandle,

        @Schema(description = "팀원 수", example = "5")
        int memberCount,

        @Schema(description = "스쿼드 목록")
        List<SquadInfo> squads
) {
    @Schema(description = "스쿼드 요약 정보")
    public record SquadInfo(
            @Schema(description = "스쿼드 ID", example = "1")
            Long squadId,

            @Schema(description = "스쿼드 이름", example = "스쿼드A")
            String name,

            @Schema(description = "기본 스쿼드 여부")
            boolean isDefault,

            @Schema(description = "추천 활성화 여부")
            boolean isActive,

            @Schema(description = "추천 활성화 요일 목록")
            List<RecommendationDayOfWeek> recommendationDays,

            @Schema(description = "추천 난이도 최소값", example = "9")
            Integer minProblemLevel,

            @Schema(description = "추천 난이도 최대값", example = "12")
            Integer maxProblemLevel,

            @Schema(description = "스쿼드 팀원 수", example = "3")
            int memberCount
    ) {
        public static SquadInfo from(Squad squad, int memberCount) {
            return new SquadInfo(
                    squad.getId(),
                    squad.getName(),
                    squad.isDefault(),
                    squad.isRecommendationActive(),
                    squad.getRecommendationDaysList(),
                    squad.getEffectiveMinProblemLevel(),
                    squad.getEffectiveMaxProblemLevel(),
                    memberCount
            );
        }
    }
}
