package com.ryu.studyhelper.recommendation.controller;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimit;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimitType;
import com.ryu.studyhelper.recommendation.dto.response.MyTodayProblemsResponse;
import com.ryu.studyhelper.recommendation.dto.response.RecommendationDetailResponse;
import com.ryu.studyhelper.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 추천 시스템 관련 REST API 컨트롤러
 * 팀별 문제 추천 이력 조회, 수동 추천 생성 등의 기능 제공
 */
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Recommendation", description = "문제 추천 API")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "수동 추천 생성",
            description = """
                    팀장이 수동으로 문제 추천을 생성합니다.
                    추천 생성 즉시 팀원들에게 이메일이 발송됩니다.
                    팀 추천 설정의 problemCount 값을 사용합니다.
                    """
    )
    @RateLimit(type = RateLimitType.SOLVED_AC)
    @PostMapping("/team/{teamId}/manual")
    @PreAuthorize("@teamService.isTeamLeader(#teamId, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<RecommendationDetailResponse>> createManualRecommendation(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        log.info("팀장 {}가 팀 {}에 수동 추천 생성", principalDetails.getMemberId(), teamId);

        RecommendationDetailResponse response =
                recommendationService.createManualRecommendation(teamId);

        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "스쿼드 수동 추천 생성",
            description = """
                    팀장이 특정 스쿼드에 수동으로 문제 추천을 생성합니다.
                    추천 생성 즉시 스쿼드 멤버들에게 이메일이 발송됩니다.
                    오늘 이미 추천이 있으면 실패합니다.
                    """
    )
    @RateLimit(type = RateLimitType.SOLVED_AC)
    @PostMapping("/team/{teamId}/squad/{squadId}/manual")
    @PreAuthorize("@teamService.isTeamLeader(#teamId, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<RecommendationDetailResponse>> createManualRecommendationForSquad(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "스쿼드 ID", example = "1")
            @PathVariable Long squadId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        log.info("팀장 {}가 팀 {}, 스쿼드 {}에 수동 추천 생성", principalDetails.getMemberId(), teamId, squadId);

        RecommendationDetailResponse response =
                recommendationService.createManualRecommendationForSquad(teamId, squadId);

        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    /**
     * @deprecated {@link #getMyTodayProblemsV2} 사용 (MemberRecommendation 기반)
     */
    @Deprecated
    @Operation(
            summary = "[Deprecated] 내 오늘의 문제 전체 조회",
            description = """
                    (Deprecated) /my/today-problems/v2 를 사용하세요.
                    로그인한 유저가 속한 모든 팀의 오늘 추천 문제를 조회합니다.
                    TeamMember 기반으로 현재 속한 팀만 조회됩니다.
                    - 중간 합류: 즉시 해당 팀의 오늘 문제 표시
                    - 팀 탈퇴/해산: 자동으로 목록에서 제외
                    """
    )
    @GetMapping("/my/today-problems")
    public ResponseEntity<ApiResponse<MyTodayProblemsResponse>> getMyTodayProblems(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Long memberId = principalDetails.getMemberId();
        log.info("사용자 {}의 오늘의 문제 전체 조회 (deprecated)", memberId);

        MyTodayProblemsResponse response = recommendationService.getMyTodayProblems(memberId);
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
}