package com.ryu.studyhelper.team.controller;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.team.dto.request.CreateTeamRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamInfoRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamVisibilityRequest;
import com.ryu.studyhelper.team.dto.response.CreateTeamResponse;
import com.ryu.studyhelper.team.dto.response.MyTeamResponse;
import com.ryu.studyhelper.team.dto.response.TeamMemberResponse;
import com.ryu.studyhelper.team.service.TeamActivityService;
import com.ryu.studyhelper.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Team", description = "팀 관리 API")
public class TeamController {

    private final TeamService teamService;
    private final TeamActivityService teamActivityService;

    @Operation(
            summary = "내가 속한 팀 목록 조회",
            description = "현재 로그인한 사용자가 속한 모든 팀의 목록을 조회합니다."
    )
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyTeamResponse>>> getMyTeams(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        List<MyTeamResponse> response = teamService.getMyTeams(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 생성",
            description = "새로운 팀을 생성합니다. 팀 생성자가 자동으로 팀 리더가 됩니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CreateTeamResponse>> create(
            @Valid @RequestBody CreateTeamRequest req,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        CreateTeamResponse response = teamService.create(req, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 멤버 목록 조회",
            description = "특정 팀의 멤버 목록을 조회합니다."
    )
    @GetMapping("/{teamId}/members")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamMembers(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        List<TeamMemberResponse> response = teamService.getTeamMembers(teamId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 탈퇴",
            description = "현재 로그인한 사용자가 팀에서 탈퇴합니다. 팀 리더는 탈퇴할 수 없습니다."
    )
    @PostMapping("/{teamId}/leaveTeam")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        teamService.leaveTeam(teamId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 삭제",
            description = "팀을 완전히 삭제합니다. 팀 리더만 삭제할 수 있습니다. 팀의 모든 데이터(멤버, 추천 기록 등)가 함께 삭제됩니다."
    )
    @PostMapping("/{teamId}/deleteTeam")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        teamService.deleteTeam(teamId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 공개/비공개 설정 변경",
            description = "팀의 공개/비공개 설정을 변경합니다. 팀장만 변경 가능합니다.",
            deprecated = true
    )
    @Deprecated(since = "2025-01", forRemoval = true)
    @PatchMapping("/{teamId}/visibility")
    public ResponseEntity<ApiResponse<Void>> updateVisibility(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamVisibilityRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        teamService.updateVisibility(teamId, request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 정보 수정",
            description = "팀의 이름, 설명, 공개/비공개 설정을 수정합니다. 팀장만 수정 가능합니다."
    )
    @PostMapping("/{teamId}/updateInfo")
    public ResponseEntity<ApiResponse<Void>> updateTeamInfo(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamInfoRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        teamService.updateTeamInfo(teamId, request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }
}
