package com.ryu.studyhelper.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class OAuthInfo {
    private String email;
    private String name;
    private String profileImageUrl;
    private String provider;
    private String providerId;

    /**
     * OAuth2User에서 Google 정보 추출
     */
    public static OAuthInfo ofGoogle(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        return OAuthInfo.builder()
                .email((String) attributes.get("email"))
                .name((String) attributes.get("name"))
                .profileImageUrl((String) attributes.get("picture"))
                .provider("google")
                .providerId((String) attributes.get("sub"))
                .build();
    }
}