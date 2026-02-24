package com.ryu.studyhelper.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "팀 리더보드 응답")
public record TeamLeaderboardResponse(
        @Schema(description = "현재 로그인 멤버 ID (비로그인 시 null)")
        Long currentMemberId,

        @Schema(description = "조회 기간 정보")
        Period period,

        @Schema(description = "멤버별 순위 (총 풀이 수 기준 내림차순)")
        List<MemberRank> memberRanks
) {
    @Schema(description = "조회 기간")
    public record Period(
            @Schema(description = "조회 일수", example = "30")
            int days,

            @Schema(description = "시작일", example = "2025-11-21")
            LocalDate startDate,

            @Schema(description = "종료일", example = "2025-12-20")
            LocalDate endDate
    ) {}

    @Schema(description = "멤버 순위 정보")
    public record MemberRank(
            @Schema(description = "멤버 ID")
            Long memberId,

            @Schema(description = "BOJ 핸들")
            String handle,

            @Schema(description = "현재 소속 스쿼드 ID (미배정 시 null)")
            Long squadId,

            @Schema(description = "현재 소속 스쿼드 이름 (미배정 시 null)")
            String squadName,

            @Schema(description = "순위 (동점자 공동 순위 처리)")
            int rank,

            @Schema(description = "기간 내 총 풀이 수")
            long totalSolved
    ) {}
}