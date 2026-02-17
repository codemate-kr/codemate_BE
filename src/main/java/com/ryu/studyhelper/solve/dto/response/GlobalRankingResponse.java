package com.ryu.studyhelper.solve.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "전체 랭킹 응답")
public record GlobalRankingResponse(
        @Schema(description = "랭킹 목록 (상위 10명)")
        List<RankEntry> rankings
) {
    @Schema(description = "랭킹 항목")
    public record RankEntry(
            @Schema(description = "순위", example = "1")
            int rank,

            @Schema(description = "BOJ 핸들", example = "tourist")
            String handle,

            @Schema(description = "푼 문제 수", example = "150")
            long solvedCount
    ) {}

    public static GlobalRankingResponse from(List<RankEntry> rankings) {
        return new GlobalRankingResponse(rankings);
    }
}
