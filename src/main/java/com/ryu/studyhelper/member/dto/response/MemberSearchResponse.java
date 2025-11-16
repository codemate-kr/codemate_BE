package com.ryu.studyhelper.member.dto.response;

import com.ryu.studyhelper.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "멤버 검색 결과 응답")
public record MemberSearchResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "백준 핸들", example = "algorithm_master")
        String handle,

        @Schema(description = "백준 핸들 인증 여부", example = "true")
        boolean verified,

        @Schema(description = "이메일 주소 (인증된 사용자만)", example = "user@example.com")
        String email
) {
    public static MemberSearchResponse from(Member member) {
        return new MemberSearchResponse(
                member.getId(),
                member.getHandle(),
                member.isVerified(),
                member.getEmail()
        );
    }
}