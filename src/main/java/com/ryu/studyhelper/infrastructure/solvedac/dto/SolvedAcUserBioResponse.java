package com.ryu.studyhelper.infrastructure.solvedac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * solved.ac 사용자 bio 응답 DTO
 */
public record SolvedAcUserBioResponse(
        @JsonProperty("handle") String handle,
        @JsonProperty("bio") String bio
) {}