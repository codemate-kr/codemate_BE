package com.ryu.studyhelper.auth;

import com.ryu.studyhelper.auth.token.RefreshCookieManager;
import com.ryu.studyhelper.auth.token.RefreshTokenService;
import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.auth.dto.AccessToken;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final RefreshCookieManager refreshCookieManager;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessToken>> refresh(HttpServletRequest request,
                                                            HttpServletResponse response) {
        String refreshToken = refreshCookieManager.read(request)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.REFRESH_TOKEN_NOT_FOUND));

        String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);

        //TODO: Refresh Token Rotation (RTR) 고려

        // (RTR을 적용하지 않는 현 설계라면 쿠키 갱신 불필요)
        // 만약 추후 RTR 도입 시:
        // String newRefreshToken = ...;
        // refreshCookieManager.write(response, newRefreshToken);

        return ResponseEntity.ok(ApiResponse.createSuccess(new AccessToken(newAccessToken, "Bearer"), CustomResponseStatus.SUCCESS));
    }
}