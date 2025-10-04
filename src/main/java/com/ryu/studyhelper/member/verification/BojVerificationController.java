package com.ryu.studyhelper.member.verification;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.member.verification.dto.GenerateHashResponse;
import com.ryu.studyhelper.member.verification.dto.VerifyBojRequest;
import com.ryu.studyhelper.member.verification.dto.VerifyBojResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 백준 핸들 인증 관련 API 컨트롤러
 * Deprecated : 본인인증 절차 생략하기로 함
 */
@RestController
@RequestMapping("/api/members/me/verification/boj")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "BOJ Verification", description = "백준 핸들 인증 API")
public class BojVerificationController {

    private final BojVerificationFacade bojVerificationFacade;

    @PostMapping("/generate-hash")
    @Operation(summary = "백준 핸들 인증용 해시 생성", description = "solved.ac 프로필의 상태 메시지에 입력할 인증 해시를 생성합니다. (유효시간: 10분)")
    public ApiResponse<GenerateHashResponse> generateVerificationHash(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long memberId = principalDetails.getMember().getId();
        log.info("Generating BOJ verification hash for member: {}", memberId);

        GenerateHashResponse response = bojVerificationFacade.generateVerificationHash(memberId);
        return ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS);
    }

    @PostMapping("/verify")
    @Operation(summary = "백준 핸들 검증", description = "solved.ac 프로필의 상태 메시지에 해시가 있는지 확인하여 백준 핸들을 인증합니다.")
    public ApiResponse<VerifyBojResponse> verifyBojHandle(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody VerifyBojRequest request
    ) {
        Long memberId = principalDetails.getMember().getId();
        log.info("Verifying BOJ handle for member: {}, handle: {}", memberId, request.handle());

        VerifyBojResponse response = bojVerificationFacade.verifyBojHandle(memberId, request.handle());
        return ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS);
    }
}