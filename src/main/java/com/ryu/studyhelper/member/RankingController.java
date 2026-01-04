package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.member.dto.response.GlobalRankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranking")
@Tag(name = "Ranking", description = "랭킹 API")
public class RankingController {

    private final RankingService rankingService;

    @Operation(
            summary = "전체 랭킹 조회",
            description = "전체 회원 중 문제를 가장 많이 푼 상위 10명의 랭킹을 조회합니다."
    )
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<GlobalRankingResponse>> getGlobalRanking() {
        GlobalRankingResponse response = rankingService.getGlobalRanking();
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }
}