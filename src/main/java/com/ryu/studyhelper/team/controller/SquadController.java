package com.ryu.studyhelper.team.controller;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.team.dto.request.CreateSquadRequest;
import com.ryu.studyhelper.team.dto.request.SquadRecommendationSettingsRequest;
import com.ryu.studyhelper.team.dto.request.UpdateMemberSquadRequest;
import com.ryu.studyhelper.team.dto.request.UpdateSquadRequest;
import com.ryu.studyhelper.team.dto.response.SquadRecommendationSettingsResponse;
import com.ryu.studyhelper.team.dto.response.SquadResponse;
import com.ryu.studyhelper.team.dto.response.SquadSummaryResponse;
import com.ryu.studyhelper.team.service.SquadService;
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
@Tag(name = "Squad", description = "팀 스쿼드 관리 API")
public class SquadController {

    private final SquadService squadService;

    @Operation(summary = "스쿼드 생성", description = "팀장 전용. 스쿼드는 최대 5개까지 생성할 수 있습니다.")
    @PostMapping("/{teamId}/squads")
    public ResponseEntity<ApiResponse<SquadResponse>> createSquad(
            @PathVariable Long teamId,
            @Valid @RequestBody CreateSquadRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        SquadResponse response = squadService.createSquad(teamId, request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "스쿼드 목록 조회", description = "팀원 이상 접근 가능")
    @GetMapping("/{teamId}/squads")
    public ResponseEntity<ApiResponse<List<SquadSummaryResponse>>> getSquads(
            @PathVariable Long teamId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        List<SquadSummaryResponse> response = squadService.getSquads(teamId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "스쿼드 정보 수정", description = "팀장 전용")
    @PutMapping("/{teamId}/squads/{squadId}")
    public ResponseEntity<ApiResponse<SquadResponse>> updateSquad(
            @PathVariable Long teamId,
            @PathVariable Long squadId,
            @Valid @RequestBody UpdateSquadRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        SquadResponse response = squadService.updateSquad(teamId, squadId, request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "스쿼드 삭제", description = "팀장 전용. 기본 스쿼드는 삭제할 수 없습니다.")
    @DeleteMapping("/{teamId}/squads/{squadId}")
    public ResponseEntity<ApiResponse<Void>> deleteSquad(
            @PathVariable Long teamId,
            @PathVariable Long squadId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        squadService.deleteSquad(teamId, squadId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "스쿼드 추천 설정 조회", description = "팀원 이상 접근 가능")
    @GetMapping("/{teamId}/squads/{squadId}/recommendation-settings")
    public ResponseEntity<ApiResponse<SquadRecommendationSettingsResponse>> getRecommendationSettings(
            @PathVariable Long teamId,
            @PathVariable Long squadId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        SquadRecommendationSettingsResponse response = squadService.getRecommendationSettings(
                teamId, squadId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "스쿼드 추천 설정 수정", description = "팀장 전용. 태그는 전체 교체됩니다.")
    @PutMapping("/{teamId}/squads/{squadId}/recommendation-settings")
    public ResponseEntity<ApiResponse<SquadRecommendationSettingsResponse>> updateRecommendationSettings(
            @PathVariable Long teamId,
            @PathVariable Long squadId,
            @Valid @RequestBody SquadRecommendationSettingsRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        SquadRecommendationSettingsResponse response = squadService.updateRecommendationSettings(
                teamId, squadId, request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "스쿼드 추천 비활성화", description = "팀장 전용")
    @DeleteMapping("/{teamId}/squads/{squadId}/recommendation-settings")
    public ResponseEntity<ApiResponse<SquadRecommendationSettingsResponse>> disableRecommendation(
            @PathVariable Long teamId,
            @PathVariable Long squadId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        SquadRecommendationSettingsResponse response = squadService.disableRecommendation(
                teamId, squadId, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "팀원 스쿼드 변경", description = "팀장 전용")
    @PutMapping("/{teamId}/members/{memberId}/squad")
    public ResponseEntity<ApiResponse<Void>> changeMemberSquad(
            @Parameter(description = "팀 ID", example = "1") @PathVariable Long teamId,
            @Parameter(description = "변경 대상 멤버 ID", example = "2") @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberSquadRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        squadService.changeMemberSquad(teamId, memberId, request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }
}
