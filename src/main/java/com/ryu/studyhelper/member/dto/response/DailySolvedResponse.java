package com.ryu.studyhelper.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "일별 문제 풀이 현황 응답")
public record DailySolvedResponse(
        @Schema(description = "일별 풀이 목록")
        List<DailySolved> dailySolved,

        @Schema(description = "총 풀이 수", example = "16")
        int totalCount
) {
    @Schema(description = "일별 풀이 정보")
    public record DailySolved(
            @Schema(description = "날짜 (오전 6시 기준)", example = "2024-11-28")
            String date,

            @Schema(description = "해당 날짜 풀이 수", example = "5")
            int count,

            @Schema(description = "풀이한 문제 목록")
            List<SolvedProblem> problems
    ) {}

    @Schema(description = "풀이한 문제 정보")
    public record SolvedProblem(
            @Schema(description = "문제 번호", example = "7576")
            Long problemId,

            @Schema(description = "문제 제목", example = "토마토")
            String title,

            @Schema(description = "문제 티어 (1~30)", example = "11")
            Integer tier
    ) {}
}
