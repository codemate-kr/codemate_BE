package com.ryu.studyhelper.member.dto.response;

import com.ryu.studyhelper.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 변경 완료 응답")
public record EmailChangeResponse(
        @Schema(description = "변경된 이메일", example = "new@example.com")
        String email
) {
    public static EmailChangeResponse from(Member member) {
        return new EmailChangeResponse(member.getEmail());
    }
}