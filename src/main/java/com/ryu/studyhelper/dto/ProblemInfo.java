package com.ryu.studyhelper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProblemInfo(
        @JsonProperty("problemId") int problemId,
        @JsonProperty("titleKo") String titleKo,
        @JsonProperty("level") int level,
        @JsonProperty("isSolvable") boolean isSolvable,
        String url // 직접 가공
) {
    public ProblemInfo withUrl() {
        return new ProblemInfo(
                problemId,
                titleKo,
                level,
                isSolvable,
                "https://www.acmicpc.net/problem/" + problemId
        );
    }
}