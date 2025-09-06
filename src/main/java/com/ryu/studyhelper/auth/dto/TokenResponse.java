package com.ryu.studyhelper.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
}
