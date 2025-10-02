package com.ryu.studyhelper.solvedac;

import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.solvedac.api.SolvedAcClient;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.solvedac.dto.SolvedAcUserResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SolvedAcService {
    private final SolvedAcClient solvedAcClient;

    public SolvedAcService(SolvedAcClient solvedAcClient) {
        this.solvedAcClient = solvedAcClient;
    }

    public SolvedAcUserResponse getUserInfo(String handle) {
        return solvedAcClient.getUserInfo(handle);
    }

    /**
     * 주어진 사용자 핸들에 대해, 1000명이상이 푼 골드문제중 풀지 않은 문제를 추천합니다.
     * @param handles 추천할 사용자 핸들 목록
     * @param count 추천할 문제 개수
     * @return 추천된 문제 목록
     */
    public List<ProblemInfo> recommendUnsolvedProblems(List<String> handles, int count) {
        String query = Stream.concat(
                        Stream.of("*g5..g1", "s#1000..", "lang:ko"),
                        handles.stream().map(h -> "!s@" + h))
                .collect(Collectors.joining("+"));
//        System.out.println(query);

        ProblemSearchResponse response = solvedAcClient.searchProblems(query, count);
        return response.items().stream()
                .map(ProblemInfo::withUrl)
                .toList();
    }

    public List<ProblemInfo> fetchSolvedProblems(String handle) {
        ProblemSearchResponse response = solvedAcClient.getSolvedProblemsRaw(handle);
        return response.items().stream()
                .map(ProblemInfo::withUrl)
                .toList();
    }







    /**
     * solved ac api 응답 시간 측정용
     * */
    public List<ProblemInfo> recommendTest(List<String> handles, int totalCount) {
        List<ProblemInfo> results = new ArrayList<>();

        int retry = 0;
        for(int i=0;i<10;i++){
            results.addAll(recommendUnsolvedProblems(handles, totalCount));
        }
        return results;
    }
}