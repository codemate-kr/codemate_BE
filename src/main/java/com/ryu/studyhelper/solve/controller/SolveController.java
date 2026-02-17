package com.ryu.studyhelper.solve.controller;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimit;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimitType;
import com.ryu.studyhelper.solve.dto.response.DailySolvedResponse;
import com.ryu.studyhelper.solve.dto.response.GlobalRankingResponse;
import com.ryu.studyhelper.solve.service.RankingService;
import com.ryu.studyhelper.solve.service.SolveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/solve")
@Tag(name = "Solve", description = "풀이 인증/랭킹 API")
public class SolveController {

    private final SolveService solveService;
    private final RankingService rankingService;

    @Operation(
            summary = "문제 해결 인증",
            description = "BOJ 문제 해결을 solved.ac API로 검증하고 인증합니다. 성공 시 MemberSolvedProblem 레코드가 생성됩니다."
    )
    @RateLimit(type = RateLimitType.SOLVED_AC)
    @PostMapping("/problems/{problemId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyProblemSolved(
            @Parameter(description = "BOJ 문제 번호", example = "1000")
            @PathVariable Long problemId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        solveService.verifyProblemSolved(principalDetails.getMemberId(), problemId);
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "일별 문제 풀이 현황 조회",
            description = "최근 N일간 일별 문제 풀이 현황을 조회합니다. 날짜 기준은 오전 6시입니다."
    )
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailySolvedResponse>> getDailySolved(
            @Parameter(description = "조회할 일수 (기본 7일)", example = "7")
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        DailySolvedResponse response = solveService.getDailySolved(principalDetails.getMemberId(), days);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "전체 랭킹 조회",
            description = "전체 회원 중 문제를 가장 많이 푼 상위 10명의 랭킹을 조회합니다."
    )
    @GetMapping("/ranking/global")
    public ResponseEntity<ApiResponse<GlobalRankingResponse>> getGlobalRanking() {
        GlobalRankingResponse response = rankingService.getGlobalRanking();
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }
}