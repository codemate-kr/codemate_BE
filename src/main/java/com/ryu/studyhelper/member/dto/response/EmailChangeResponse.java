package com.ryu.studyhelper.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 변경 완료 응답")
public record EmailChangeResponse(
        @Schema(description = "변경된 이메일", example = "new@example.com")
        String email
) {
}