package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimit;
import com.ryu.studyhelper.infrastructure.ratelimit.RateLimitType;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.dto.request.*;
import com.ryu.studyhelper.member.dto.response.*;
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
        MyProfileResponse response = memberService.getMyProfile(principalDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
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
            description = "백준 핸들로 멤버를 검색합니다. 중복 핸들이 허용되므로 여러 명의 결과가 반환될 수 있습니다. 결과가 없을 경우 빈 배열을 반환합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MemberSearchResponse>>> searchByHandle(
            @Parameter(description = "백준 핸들", example = "algorithm_master")
            @RequestParam String handle) {
        List<MemberSearchResponse> responses = memberService.searchByHandle(handle);
        return ResponseEntity.ok(ApiResponse.createSuccess(responses, CustomResponseStatus.SUCCESS));
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
            description = "백준 핸들을 인증하여 사용자와 연결합니다. 인증 성공시 업데이트된 핸들을 반환합니다."
    )
    @RateLimit(type = RateLimitType.SOLVED_AC)
    @PostMapping("/me/verify-solvedac")
    public ResponseEntity<ApiResponse<HandleVerificationResponse>> verifySolvedAc(
            @Valid @RequestBody VerifySolvedAcRequest req,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Member updated = memberService.verifySolvedAcHandle(principalDetails.getMemberId(), req.handle());
        return ResponseEntity.ok(ApiResponse.createSuccess(HandleVerificationResponse.from(updated), CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "이메일 중복 검사",
            description = "변경하려는 이메일이 이미 사용 중인지 확인합니다."
    )
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<CheckEmailResponse>> checkEmail(
            @Valid @RequestBody CheckEmailRequest req
    ) {
        boolean available = memberService.isEmailAvailable(req.email());
        return ResponseEntity.ok(ApiResponse.createSuccess(
                new CheckEmailResponse(available),
                CustomResponseStatus.SUCCESS
        ));
    }

    @Operation(
            summary = "이메일 변경 인증 메일 발송",
            description = "새로운 이메일로 인증 링크를 발송합니다. 링크는 5분간 유효합니다."
    )
    @PostMapping("/me/send-email-verification")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(
            @Valid @RequestBody SendEmailVerificationRequest req,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        memberService.sendEmailVerification(principalDetails.getMemberId(), req.email());
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "이메일 변경 완료",
            description = "이메일 인증 토큰을 검증하고 이메일을 변경합니다. 프론트엔드가 이메일 링크의 토큰을 추출하여 호출합니다."
    )
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<EmailChangeResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest req
    ) {
        String newEmail = memberService.verifyAndChangeEmail(req.token());
        return ResponseEntity.ok(ApiResponse.createSuccess(new EmailChangeResponse(newEmail), CustomResponseStatus.SUCCESS));
    }

    @Operation(
            summary = "문제 해결 인증",
            description = "BOJ 문제 해결을 solved.ac API로 검증하고 인증합니다. 성공 시 MemberSolvedProblem 레코드가 생성됩니다."
    )
    @RateLimit(type = RateLimitType.SOLVED_AC)
    @PostMapping("/me/problems/{problemId}/verify-solved")
    public ResponseEntity<ApiResponse<Void>> verifyProblemSolved(
            @Parameter(description = "BOJ 문제 번호", example = "1000")
            @PathVariable Long problemId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        memberService.verifyProblemSolved(principalDetails.getMemberId(), problemId);
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }
}
