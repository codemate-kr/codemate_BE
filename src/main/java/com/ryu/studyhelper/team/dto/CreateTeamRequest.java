package com.ryu.studyhelper.team.dto;

import com.ryu.studyhelper.member.domain.Member;
import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
        @NotBlank String name,
        String description
) {}