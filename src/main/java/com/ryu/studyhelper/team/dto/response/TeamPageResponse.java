package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "팀 페이지 통합 응답")
public record TeamPageResponse(
        @Schema(description = "팀 기본 정보")
        TeamInfo team,

        @Schema(description = "팀 멤버 목록")
        List<TeamMemberResponse> members,

        @Schema(description = "추천 설정")
        TeamRecommendationSettingsResponse recommendationSettings,

        @Schema(description = "오늘의 문제 (없으면 null)")
        TodayProblemResponse todayProblem
) {
    @Schema(description = "팀 기본 정보")
    public record TeamInfo(
            @Schema(description = "팀 ID", example = "1")
            Long teamId,

            @Schema(description = "팀 이름", example = "알고리즘 스터디")
            String teamName,

            @Schema(description = "팀 설명")
            String description,

            @Schema(description = "비공개 팀 여부", example = "false")
            boolean isPrivate,

            @Schema(description = "팀원 수", example = "5")
            int memberCount
    ) {}
}