package com.ryu.studyhelper.team.controller.v2;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.team.dto.response.MyTeamResponse;
import com.ryu.studyhelper.team.dto.response.PublicTeamResponseV2;
import com.ryu.studyhelper.team.dto.response.TeamActivityResponseV2;
import com.ryu.studyhelper.team.dto.response.TeamLeaderboardResponse;
import com.ryu.studyhelper.team.dto.response.TeamPageResponseV2;
import com.ryu.studyhelper.team.service.TeamActivityService;
import com.ryu.studyhelper.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/teams")
@RequiredArgsConstructor
@Tag(name = "TeamV2", description = "팀 v2 API")
public class TeamControllerV2 {

    private final TeamService teamService;
    private final TeamActivityService teamActivityService;

    @Operation(summary = "v2 팀 페이지 조회")
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamPageResponseV2>> getTeamPageDetailV2(
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long memberId = principalDetails != null ? principalDetails.getMemberId() : null;
        TeamPageResponseV2 response = teamService.getTeamPageDetailV2(teamId, memberId);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "v2 공개 팀 목록 조회", description = "스쿼드별 추천 설정 포함")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PublicTeamResponseV2>>> getPublicTeams() {
        List<PublicTeamResponseV2> response = teamService.getPublicTeamsV2();
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "v2 내 팀 목록 조회")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyTeamResponse>>> getMyTeams(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        List<MyTeamResponse> response = teamService.getMyTeams(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "v2 팀 리더보드 조회",
            description = "기간 내 팀원 풀이 수 기준 순위 반환. 스쿼드 필터링은 클라이언트에서 처리.")
    @GetMapping("/{teamId}/leaderboard")
    public ResponseEntity<ApiResponse<TeamLeaderboardResponse>> getTeamLeaderboard(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "30") Integer days,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long memberId = principalDetails != null ? principalDetails.getMemberId() : null;
        TeamLeaderboardResponse response = teamActivityService.getTeamLeaderboard(teamId, memberId, days);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "v2 팀 활동 현황 조회",
            description = "스쿼드 단위 일별 활동 현황 반환. 리더보드는 /leaderboard 엔드포인트를 사용하세요.")
    @GetMapping("/{teamId}/activity")
    public ResponseEntity<ApiResponse<TeamActivityResponseV2>> getTeamActivityV2(
            @PathVariable Long teamId,
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long memberId = principalDetails != null ? principalDetails.getMemberId() : null;
        TeamActivityResponseV2 response = teamActivityService.getTeamActivityV2(teamId, memberId, days);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }
}
