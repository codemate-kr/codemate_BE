package com.ryu.studyhelper.member.dto;

import com.ryu.studyhelper.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "멤버 공개 정보 응답 (다른 사용자 조회용)")
public record MemberPublicResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "백준 핸들", example = "algorithm_master")
        String handle,

        @Schema(description = "백준 핸들 인증 여부", example = "true")
        boolean verified
) {
    public static MemberPublicResponse from(Member member) {
        return new MemberPublicResponse(
                member.getId(),
                member.getHandle(),
                member.isVerified()
        );
    }
}