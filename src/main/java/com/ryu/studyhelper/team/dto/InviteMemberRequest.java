package com.ryu.studyhelper.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 멤버 초대 요청 DTO
 */
@Schema(description = "멤버 초대 요청")
public record InviteMemberRequest(
        @NotNull(message = "memberId는 필수입니다")
        @Schema(description = "초대할 멤버의 ID", example = "1")
        Long memberId
) {
}