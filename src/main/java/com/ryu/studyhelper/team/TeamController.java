package com.ryu.studyhelper.team;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.team.dto.request.CreateTeamRequest;
import com.ryu.studyhelper.team.dto.request.InviteMemberRequest;
import com.ryu.studyhelper.team.dto.request.TeamRecommendationSettingsRequest;
import com.ryu.studyhelper.team.dto.response.CreateTeamResponse;
import com.ryu.studyhelper.team.dto.response.InviteMemberResponse;
import com.ryu.studyhelper.team.dto.response.MyTeamResponse;
import com.ryu.studyhelper.team.dto.response.TeamMemberResponse;
import com.ryu.studyhelper.team.dto.response.TeamPageResponse;
import com.ryu.studyhelper.team.dto.response.TeamRecommendationSettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Team", description = "팀 관리 API")
public class TeamController {

    private final TeamService teamService;

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
            summary = "팀 페이지 통합 조회",
            description = """
                    팀 페이지에 필요한 모든 정보를 한번에 조회합니다.
                    - 팀 기본 정보, 멤버 목록, 추천 설정, 오늘의 문제 포함
                    - 비공개 팀인 경우 팀원만 접근 가능합니다.
                    """
    )
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamPageResponse>> getTeamPageDetail(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Long memberId = principalDetails != null ? principalDetails.getMemberId() : null;
        TeamPageResponse response = teamService.getTeamPageDetail(teamId, memberId);
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
            summary = "팀 추천 설정 조회",
            description = "특정 팀의 문제 추천 설정(요일, 난이도)을 조회합니다. 팀 멤버만 조회 가능합니다."
    )
    @GetMapping("/{teamId}/recommendation-settings")
    public ResponseEntity<ApiResponse<TeamRecommendationSettingsResponse>> getRecommendationSettings(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId) {

        TeamRecommendationSettingsResponse response = teamService.getRecommendationSettings(teamId);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 추천 설정 업데이트",
            description = "팀의 문제 추천 요일과 난이도를 설정합니다. 팀장만 설정 가능합니다."
    )
    @PutMapping("/{teamId}/recommendation-settings")
    @PreAuthorize("@teamService.isTeamLeader(#teamId, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<TeamRecommendationSettingsResponse>> updateRecommendationSettings(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody TeamRecommendationSettingsRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        TeamRecommendationSettingsResponse response = teamService.updateRecommendationSettings(
                teamId, request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "팀 추천 비활성화",
            description = "팀의 문제 추천을 비활성화합니다. 팀장만 설정 가능합니다."
    )
    @DeleteMapping("/{teamId}/recommendation-settings")
    @PreAuthorize("@teamService.isTeamLeader(#teamId, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<TeamRecommendationSettingsResponse>> disableRecommendation(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        TeamRecommendationSettingsResponse response = teamService.disableRecommendation(
                teamId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "멤버 초대",
            description = "선택한 멤버를 팀에 초대합니다. 팀장만 초대 가능합니다. 초대된 멤버는 알림을 받습니다."
    )
    @PostMapping("/{teamId}/invite")
    @PreAuthorize("@teamService.isTeamLeader(#teamId, authentication.principal.memberId)")
    public ResponseEntity<ApiResponse<InviteMemberResponse>> inviteMember(
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        InviteMemberResponse response = teamService.inviteMember(teamId, request, principalDetails.getMemberId());
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
}