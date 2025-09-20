package com.ryu.studyhelper.auth;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.auth.dto.AccessToken;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * /oauth2/authorization/{provider} OAuth2 인증 과정은
     * Spring Security가 처리하므로, 여기서는 리프레시 토큰을 이용한
     * 액세스 토큰 재발급(refresh)만 처리한다.
     */

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessToken>> refresh(HttpServletRequest request) {
        AccessToken accessToken = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.createSuccess(accessToken, CustomResponseStatus.SUCCESS));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader,
                                                    HttpServletResponse response) {
        String accessToken = authHeader.replace("Bearer ", "");
        authService.logout(accessToken, response);

        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }
}