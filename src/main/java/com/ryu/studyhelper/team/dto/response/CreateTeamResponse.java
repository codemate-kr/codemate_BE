package com.ryu.studyhelper.team.dto.response;

public record CreateTeamResponse(
        String name,
        String description
) {

    public static CreateTeamResponse from(String name, String description) {
        return new CreateTeamResponse(name, description);
    }
}