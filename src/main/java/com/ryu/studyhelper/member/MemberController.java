package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.dto.MemberPublicResponse;
import com.ryu.studyhelper.member.dto.MemberSearchResponse;
import com.ryu.studyhelper.member.dto.MyProfileResponse;
import com.ryu.studyhelper.member.dto.VerifySolvedAcRequest;
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
@RequiredArgsConstructor
@RequestMapping("/api/member")
@Tag(name = "Member", description = "회원 관리 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 사용자의 상세 프로필 정보를 조회합니다. 민감 정보가 포함됩니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> me(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Member me = memberService.getById(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(MyProfileResponse.from(me), CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "멤버 공개 정보 조회",
            description = "특정 멤버의 공개 정보만 조회합니다. 민감 정보는 제외됩니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberPublicResponse>> getById(
            @Parameter(description = "멤버 ID", example = "1")
            @PathVariable Long id) {
        Member member = memberService.getById(id);
        return ResponseEntity.ok(ApiResponse.createSuccess(MemberPublicResponse.from(member), CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "핸들로 멤버 검색",
            description = "백준 핸들로 멤버를 검색합니다. 인증된 사용자의 경우 이메일도 포함됩니다."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<MemberSearchResponse>> getByHandle(
            @Parameter(description = "백준 핸들", example = "algorithm_master")
            @RequestParam String handle) {
        Member member = memberService.getByHandle(handle);
        return ResponseEntity.ok(ApiResponse.createSuccess(MemberSearchResponse.from(member), CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "인증된 핸들 목록 조회",
            description = "백준 핸들이 인증된 모든 사용자의 핸들 목록을 조회합니다."
    )
    @GetMapping("/handles")
    public ResponseEntity<ApiResponse<List<String>>> getVerifiedHandles() {
        List<String> handles = memberService.getVerifiedHandles();
        return ResponseEntity.ok(ApiResponse.createSuccess(handles, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "백준 핸들 인증",
            description = "백준 핸들을 인증하여 사용자와 연결합니다. 인증 성공시 업데이트된 프로필을 반환합니다."
    )
    @PostMapping("/me/verify-solvedac")
    public ResponseEntity<ApiResponse<MyProfileResponse>> verifySolvedAc(
            @Valid @RequestBody VerifySolvedAcRequest req,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Member updated = memberService.verifySolvedAcHandle(principalDetails.getMemberId(), req.handle());
        return ResponseEntity.ok(ApiResponse.createSuccess(MyProfileResponse.from(updated), CustomResponseStatus.SUCCESS));
    }
}
