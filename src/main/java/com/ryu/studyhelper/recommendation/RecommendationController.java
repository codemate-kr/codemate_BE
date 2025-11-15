package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.recommendation.dto.response.TeamRecommendationDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 추천 시스템 관련 REST API 컨트롤러
 * 팀별 문제 추천 이력 조회, 수동 추천 생성 등의 기능 제공
 */
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recommendation", description = "문제 추천 API")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "수동 추천 생성",
            description = """
                    팀장이 수동으로 문제 추천을 생성합니다.
                    추천 생성 즉시 팀원들에게 이메일이 발송됩니다.
                    """
    )
    @PostMapping("/team/{teamId}/manual")
    @PreAuthorize("@teamService.isTeamLeader(#teamId, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<TeamRecommendationDetailResponse>> createManualRecommendation(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "추천 문제 개수", example = "3")
            @RequestParam(defaultValue = "3") int count,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        log.info("팀장 {}가 팀 {}에 수동 추천 생성 ({}개)", principalDetails.getMemberId(), teamId, count);

        TeamRecommendationDetailResponse response =
                recommendationService.createManualRecommendation(teamId, count);

        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "오늘의 문제 조회",
            description = """
                    특정 팀의 오늘 추천된 문제를 조회합니다.
                    가장 최근 추천(수동 추천 우선)을 반환합니다.
                    팀 멤버만 조회 가능합니다.
                    """
    )
    @GetMapping("/team/{teamId}/today-problem")
    public ResponseEntity<ApiResponse<TeamRecommendationDetailResponse>> getTodayRecommendation(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        log.info("사용자 {}가 팀 {}의 오늘의 문제 조회", principalDetails.getMemberId(), teamId);

        TeamRecommendationDetailResponse response =
                recommendationService.getTodayRecommendation(teamId, principalDetails.getMemberId());

        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "추천 시스템 상태 확인",
            description = "추천 시스템의 현재 상태를 확인합니다."
    )
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.createSuccess("추천 시스템이 정상 작동 중입니다.", CustomResponseStatus.SUCCESS));
    }

    // TODO: 별도 티켓에서 구현 예정 - RecommendationProblem + MemberSolvedProblem JOIN으로 변경
    // @Operation(
    //         summary = "개인 대시보드 - 오늘의 모든 추천 문제 조회",
    //         description = """
    //                 유저가 속한 모든 팀의 오늘 추천 문제들을 조회합니다.
    //                 각 문제별로 팀명과 해결 여부가 함께 표시됩니다.
    //                 대시보드에서 오늘 풀어야 할 모든 문제를 한눈에 볼 수 있습니다.
    //                 """
    // )
    // @GetMapping("/my/today-problem")
    // public ResponseEntity<ApiResponse<MyTodayRecommendationResponse>> getMyTodayRecommendations(
    //         @AuthenticationPrincipal PrincipalDetails principalDetails) {
    //
    //     log.info("사용자 {}가 오늘의 모든 추천 문제 조회", principalDetails.getMemberId());
    //
    //     MyTodayRecommendationResponse response =
    //             recommendationService.getMyTodayRecommendations(principalDetails.getMemberId());
    //
    //     return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    // }
}