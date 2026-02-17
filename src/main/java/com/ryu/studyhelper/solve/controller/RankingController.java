package com.ryu.studyhelper.solve.controller;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.solve.dto.response.GlobalRankingResponse;
import com.ryu.studyhelper.solve.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 프론트엔드 마이그레이션 후 제거 → SolveController(/api/solve/ranking/global)로 통합 완료
@Deprecated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranking")
@Tag(name = "Ranking (Deprecated)", description = "랭킹 API — /api/solve/ranking/global 로 이전됨")
public class RankingController {

    private final RankingService rankingService;

    @Deprecated
    @Operation(summary = "[Deprecated] 전체 랭킹 조회", description = "GET /api/solve/ranking/global 로 이전됨")
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<GlobalRankingResponse>> getGlobalRanking() {
        GlobalRankingResponse response = rankingService.getGlobalRanking();
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }
}