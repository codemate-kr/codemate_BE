package com.ryu.studyhelper.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 팀 활동 현황 응답 DTO
 * 팀원들의 문제 풀이 현황과 리더보드를 포함합니다.
 */
@Schema(description = "팀 활동 현황 응답")
public record TeamActivityResponse(
        @Schema(description = "현재 로그인 멤버 ID (비로그인 시 null)")
        Long currentMemberId,

        @Schema(description = "조회 기간 정보")
        Period period,

        @Schema(description = "멤버별 리더보드 (총 풀이 수 기준 정렬)")
        List<MemberRank> members,

        @Schema(description = "일별 활동 현황 (최신순)")
        List<DailyActivity> dailyActivities
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
            @Schema(description = "멤버 ID", example = "1")
            Long memberId,

            @Schema(description = "BOJ 핸들", example = "ryu_boj")
            String handle,

            @Schema(description = "순위", example = "1")
            int rank,

            @Schema(description = "기간 내 총 풀이 수", example = "25")
            int totalSolved
    ) {}

    @Schema(description = "일별 활동 현황")
    public record DailyActivity(
            @Schema(description = "날짜", example = "2025-12-20")
            LocalDate date,

            @Schema(description = "해당 일자 추천 문제 목록")
            List<ProblemInfo> problems,

            @Schema(description = "멤버별 풀이 현황")
            List<MemberSolved> memberSolved
    ) {}

    @Schema(description = "문제 정보")
    public record ProblemInfo(
            @Schema(description = "문제 ID", example = "1001")
            Long problemId,

            @Schema(description = "문제 제목", example = "이분탐색")
            String title,

            @Schema(description = "문제 난이도 (tier)", example = "11")
            Integer tier
    ) {}

    @Schema(description = "멤버 풀이 현황")
    public record MemberSolved(
            @Schema(description = "멤버 ID", example = "1")
            Long memberId,

            @Schema(description = "문제별 풀이 여부 (problemId -> solved)")
            Map<Long, Boolean> solved
    ) {}

    public static TeamActivityResponse of(
            Long currentMemberId,
            Period period,
            List<MemberRank> members,
            List<DailyActivity> dailyActivities) {
        return new TeamActivityResponse(currentMemberId, period, members, dailyActivities);
    }
}