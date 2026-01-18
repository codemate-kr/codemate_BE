package com.ryu.studyhelper.solvedac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ryu.studyhelper.common.util.ProblemUrlUtils;

import java.util.List;

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

        String url, // 직접 가공

        @JsonProperty("tags")
        List<SolvedAcTagInfo> tags

) {
    public ProblemInfo withUrl() {
        return new ProblemInfo(
                problemId,
                titleKo,
                level,
                acceptedUserCount,
                averageTries,
                ProblemUrlUtils.generateProblemUrl(problemId),
                tags
        );
    }
}