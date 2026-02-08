package com.ryu.studyhelper.problem.service;


import com.ryu.studyhelper.problem.dto.ProblemRecommendRequest;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class ProblemService {

    private final SolvedAcClient solvedAcClient;

    /**
     * 문제 추천
     */
    public List<ProblemInfo> recommend(List<String> handles, int count, Integer minLevel, Integer maxLevel, List<String> tagKeys) {
        return solvedAcClient.recommendUnsolvedProblems(handles, count, minLevel, maxLevel, tagKeys);
    }

    public List<ProblemInfo> recommend(ProblemRecommendRequest request, int count) {
        return recommend(request.handles(), count, null, null, null);
    }

    public List<ProblemInfo> recommend(String handle, int count) {
        return recommend(List.of(handle), count, null, null, null);
    }

    public List<ProblemInfo> recommend(List<String> handles, int count) {
        return recommend(handles, count, null, null, null);
    }
}