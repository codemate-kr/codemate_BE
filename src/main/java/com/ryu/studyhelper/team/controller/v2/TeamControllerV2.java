package com.ryu.studyhelper.team.controller.v2;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.team.dto.response.MyTeamResponse;
import com.ryu.studyhelper.team.dto.response.PublicTeamResponse;
import com.ryu.studyhelper.team.dto.response.TeamPageResponseV2;
import com.ryu.studyhelper.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/teams")
@RequiredArgsConstructor
@Tag(name = "TeamV2", description = "팀 v2 API")
public class TeamControllerV2 {

    private final TeamService teamService;

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

    @Operation(summary = "v2 공개 팀 목록 조회")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PublicTeamResponse>>> getPublicTeams() {
        List<PublicTeamResponse> response = teamService.getPublicTeams();
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
}
