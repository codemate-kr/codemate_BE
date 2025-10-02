package com.ryu.studyhelper.solvedac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProblemInfo(
        @JsonProperty("problemId")
        Long problemId,

        @JsonProperty("titleKo")
        String titleKo,

        @JsonProperty("level")
        int level,

        @JsonProperty("acceptedUserCount")
        int acceptedUserCount,

        @JsonProperty("averageTries")
        double averageTries,

        String url // 직접 가공

//        @JsonProperty("tags")
//        List<Tag> tags,

) {
    public ProblemInfo withUrl() {
        return new ProblemInfo(
                problemId,
                titleKo,
                level,
                acceptedUserCount,
                averageTries,
                "https://www.acmicpc.net/problem/" + problemId
        );
    }
}