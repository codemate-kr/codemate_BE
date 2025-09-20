package com.ryu.studyhelper.team.dto;

import com.ryu.studyhelper.member.domain.Member;
import jakarta.validation.constraints.NotBlank;

public record CreateTeamResponse(
        @NotBlank String name,
        String description
) {

    public static CreateTeamResponse from(String name, String description) {
        return new CreateTeamResponse(name, description);
    }
}