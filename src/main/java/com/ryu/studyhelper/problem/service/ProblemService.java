package com.ryu.studyhelper.problem.service;

import com.ryu.studyhelper.problem.dto.ProblemRecommendRequest;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class ProblemService {
    private final SolvedAcService solvedAcService;

    /**
     * 핸들 목록을 기반으로 문제 추천
     */
    public List<ProblemInfo> recommend(List<String> handles, int count) {
        return solvedAcService.recommendUnsolvedProblems(handles, count);
    }

    /**
     * 핸들 목록과 난이도 범위를 기반으로 문제 추천
     */
    public List<ProblemInfo> recommend(List<String> handles, int count, Integer minLevel, Integer maxLevel) {
        return solvedAcService.recommendUnsolvedProblems(handles, count, minLevel, maxLevel);
    }

    /**
     * 핸들 목록, 난이도 범위, 태그 필터를 기반으로 문제 추천
     */
    public List<ProblemInfo> recommend(List<String> handles, int count, Integer minLevel, Integer maxLevel, List<String> tagKeys) {
        return solvedAcService.recommendUnsolvedProblems(handles, count, minLevel, maxLevel, tagKeys);
    }



    public List<ProblemInfo> recommend(ProblemRecommendRequest request, int count) {
        return recommend(request.handles(), count);
    }
    public List<ProblemInfo> recommend(String handle , int count) {
        return recommend(List.of(handle), count);
    }
}