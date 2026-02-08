package com.ryu.studyhelper.infrastructure.solvedac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SolvedAcUserResponse(
        @JsonProperty("handle") String handle,
        @JsonProperty("tier") int tier,
        @JsonProperty("solvedCount") int solvedCount,
        @JsonProperty("maxStreak") int maxStreak,
        @JsonProperty("rating") int rating
) {}