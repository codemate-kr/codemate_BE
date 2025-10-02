package com.ryu.studyhelper.member.dto;

import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 프로필 정보 응답 (민감 정보 포함)")
public record MyProfileResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "이메일 주소", example = "user@example.com")
        String email,

        @Schema(description = "백준 핸들", example = "algorithm_master")
        String handle,

        @Schema(description = "백준 핸들 인증 여부", example = "true")
        boolean verified,

        @Schema(description = "사용자 역할", example = "ROLE_USER")
        Role role,

        @Schema(description = "OAuth 제공자", example = "google")
        String provider,

        @Schema(description = "OAuth 제공자 ID")
        String providerId,

        @Schema(description = "가입일시")
        LocalDateTime joinedAt
) {
    public static MyProfileResponse from(Member member) {
        return new MyProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getHandle(),
                member.isVerified(),
                member.getRole(),
                member.getProvider(),
                member.getProviderId(),
                member.getCreatedAt()
        );
    }
}