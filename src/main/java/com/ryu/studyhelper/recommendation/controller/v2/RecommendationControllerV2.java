package com.ryu.studyhelper.recommendation.controller.v2;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.recommendation.dto.response.MyTodayProblemsResponse;
import com.ryu.studyhelper.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 API v2 컨트롤러
 * 스쿼드 도입 이후 MemberRecommendation 기반 API
 */
@RestController
@RequestMapping("/api/v2/recommendation")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Recommendation V2", description = "문제 추천 API v2 (스쿼드 기반)")
public class RecommendationControllerV2 {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "내 오늘의 문제 전체 조회",
            description = """
                    로그인한 유저의 오늘 추천 문제를 MemberRecommendation 기반으로 조회합니다.
                    스쿼드 도입 이후의 기준 API입니다.
                    TeamMember가 아닌 MemberRecommendation 기준이므로,
                    오늘 추천을 받은 시점에 팀을 탈퇴하더라도 오늘 받은 추천은 조회됩니다.
                    """
    )
    @GetMapping("/my/today-problems")
    public ResponseEntity<ApiResponse<MyTodayProblemsResponse>> getMyTodayProblems(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Long memberId = principalDetails.getMemberId();

        MyTodayProblemsResponse response = recommendationService.getMyTodayProblemsV2(memberId);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }
}