package com.ryu.studyhelper.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 팀 활동 현황 V2 응답 DTO
 * 팀원별 추천 이력과 문제 풀이 현황을 제공합니다.
 */
@Schema(description = "팀 활동 현황 V2 응답")
public record TeamActivityResponseV2(
        @Schema(description = "현재 로그인 멤버 ID (비로그인 시 null)")
        Long currentMemberId,

        @Schema(description = "조회 기간 정보")
        Period period,

        @Schema(description = "팀원별 활동 현황 목록")
        List<MemberActivity> memberActivities
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

    @Schema(description = "팀원별 활동 현황")
    public record MemberActivity(
            @Schema(description = "멤버 ID")
            Long memberId,

            @Schema(description = "BOJ 핸들")
            String handle,

            @Schema(description = "소속 스쿼드 ID (미배정 시 null)")
            Long squadId,

            @Schema(description = "소속 스쿼드 이름 (미배정 시 null)")
            String squadName,

            @Schema(description = "일별 추천 목록 (최신순, 추천 없는 날 생략)")
            List<DailyRecommendation> dailyRecommendations
    ) {}

    @Schema(description = "일별 추천 현황")
    public record DailyRecommendation(
            @Schema(description = "날짜", example = "2025-12-20")
            LocalDate date,

            @Schema(description = "추천 문제 및 풀이 현황")
            List<ProblemActivity> problems
    ) {}

    @Schema(description = "문제 활동 정보")
    public record ProblemActivity(
            @Schema(description = "문제 ID", example = "1001")
            Long problemId,

            @Schema(description = "문제 영문 제목")
            String title,

            @Schema(description = "문제 한국어 제목 (없으면 영문 제목과 동일)")
            String titleKo,

            @Schema(description = "문제 난이도 (tier)", example = "11")
            Integer tier,

            @Schema(description = "풀이 여부")
            boolean solved
    ) {}
}
