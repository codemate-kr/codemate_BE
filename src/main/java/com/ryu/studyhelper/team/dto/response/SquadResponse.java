package com.ryu.studyhelper.team.dto.response;

import com.ryu.studyhelper.team.domain.Squad;

public record SquadResponse(
        Long squadId,
        String name,
        String description,
        boolean isDefault
) {
    public static SquadResponse from(Squad squad) {
        return new SquadResponse(
                squad.getId(),
                squad.getName(),
                squad.getDescription(),
                squad.isDefault()
        );
    }
}
