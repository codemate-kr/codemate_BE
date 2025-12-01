package com.ryu.studyhelper.recommendation;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimit;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimitType;
import com.ryu.studyhelper.recommendation.dto.response.TeamRecommendationDetailResponse;
import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
                    count 파라미터가 없으면 팀 추천 설정의 problemCount 값을 사용합니다.
                    """
    )
    @RateLimit(type = RateLimitType.SOLVED_AC)
    @PostMapping("/team/{teamId}/manual")
    @PreAuthorize("@teamService.isTeamLeader(#teamId, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<TeamRecommendationDetailResponse>> createManualRecommendation(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "추천 문제 개수 (1~10, 미지정 시 팀 설정값 사용)", example = "3")
            @RequestParam(required = false) @Min(1) @Max(10) Integer count,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        log.info("팀장 {}가 팀 {}에 수동 추천 생성 (count={})", principalDetails.getMemberId(), teamId, count);

        TeamRecommendationDetailResponse response =
                recommendationService.createManualRecommendation(teamId, count);

        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "오늘의 문제 조회",
            description = """
                    특정 팀의 오늘 추천된 문제를 조회합니다.
                    가장 최근 추천(수동 추천 우선)을 반환합니다.
                    로그인한 사용자의 경우 해결 여부(isSolved)가 포함됩니다.
                    비로그인 시 isSolved는 null입니다.
                    """
    )
    @GetMapping("/team/{teamId}/today-problem")
    public ResponseEntity<ApiResponse<TodayProblemResponse>> getTodayRecommendation(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Long memberId = principalDetails != null ? principalDetails.getMemberId() : null;

        if (memberId != null) {
            log.info("사용자 {}가 팀 {}의 오늘의 문제 조회", memberId, teamId);
        } else {
            log.info("비로그인 사용자가 팀 {}의 오늘의 문제 조회", teamId);
        }

        TodayProblemResponse response = recommendationService.getTodayRecommendation(teamId, memberId);
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