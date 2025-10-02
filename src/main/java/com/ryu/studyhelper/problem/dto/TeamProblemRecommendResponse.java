package com.ryu.studyhelper.problem.dto;

import com.ryu.studyhelper.solvedac.dto.ProblemInfo;

import java.util.List;

public record TeamProblemRecommendResponse(
        List<ProblemInfo> problems,
        List<String> handles
) {
    public static TeamProblemRecommendResponse from(List<ProblemInfo> problems, List<String> handles) {
        return new TeamProblemRecommendResponse(problems, handles);
    }
}
