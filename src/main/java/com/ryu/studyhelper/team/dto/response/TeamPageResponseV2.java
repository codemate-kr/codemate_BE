package com.ryu.studyhelper.team.dto.response;

import java.util.List;

public record TeamPageResponseV2(
        TeamInfo team,
        List<TeamMemberResponse> members,
        List<SquadSummaryResponse> squads
) {
    public record TeamInfo(
            Long id,
            String name,
            String description,
            boolean isPrivate,
            int memberCount
    ) {}
}
