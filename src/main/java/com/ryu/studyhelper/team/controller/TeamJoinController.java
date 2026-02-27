package com.ryu.studyhelper.team.controller;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.team.dto.request.TeamJoinInviteRequest;
import com.ryu.studyhelper.team.dto.response.TeamJoinResponse;
import com.ryu.studyhelper.team.service.TeamJoinService;
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
@RequestMapping("/api/team-joins")
@RequiredArgsConstructor
@Tag(name = "TeamJoin", description = "팀 초대/가입 요청 API")
public class TeamJoinController {

    private final TeamJoinService teamJoinService;

    @Operation(summary = "멤버 초대", description = "팀장이 멤버를 팀에 초대합니다.")
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<TeamJoinResponse>> invite(
            @Valid @RequestBody TeamJoinInviteRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        TeamJoinResponse response = teamJoinService.inviteMember(request, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "요청 수락", description = "받은 초대를 수락합니다.")
    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<TeamJoinResponse>> accept(
            @Parameter(description = "요청 ID") @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        TeamJoinResponse response = teamJoinService.accept(id, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "요청 거절", description = "받은 초대를 거절합니다.")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<TeamJoinResponse>> reject(
            @Parameter(description = "요청 ID") @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        TeamJoinResponse response = teamJoinService.reject(id, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "요청 취소", description = "보낸 초대를 취소합니다.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TeamJoinResponse>> cancel(
            @Parameter(description = "요청 ID") @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        TeamJoinResponse response = teamJoinService.cancel(id, principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "받은 요청 목록", description = "PENDING 상태인 받은 초대 목록을 조회합니다.")
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<TeamJoinResponse>>> getReceivedList(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<TeamJoinResponse> response = teamJoinService.getReceivedList(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }

    @Operation(summary = "보낸 요청 목록", description = "PENDING 상태인 보낸 초대 목록을 조회합니다.")
    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<List<TeamJoinResponse>>> getSentList(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<TeamJoinResponse> response = teamJoinService.getSentList(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }
}