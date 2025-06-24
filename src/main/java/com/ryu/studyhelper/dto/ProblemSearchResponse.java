package com.ryu.studyhelper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProblemSearchResponse(
        @JsonProperty("items") List<ProblemInfo> items
) {}