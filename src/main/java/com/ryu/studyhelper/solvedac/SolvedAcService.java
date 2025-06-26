package com.ryu.studyhelper.solvedac;

import com.ryu.studyhelper.dto.ProblemInfo;
import com.ryu.studyhelper.solvedac.dto.SolvedAcUserResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolvedAcService {
    private final SolvedAcClient solvedAcClient;

    public SolvedAcService(SolvedAcClient solvedAcClient) {
        this.solvedAcClient = solvedAcClient;
    }

    public SolvedAcUserResponse getUserInfo(String handle) {
        return solvedAcClient.getUserInfo(handle);
    }

    public List<ProblemInfo> recommendUnsolvedProblems(List<String> handles, int count) {
        String unsolvedClause = handles.stream()
                .map(h -> "!s@" + h) // !s@handle: 해당 사용자가 안 푼 문제만 포함
                .collect(Collectors.joining("+"));

        String query = "*g5..g1+s#1000..+lang:ko+" + unsolvedClause;

        return solvedAcClient.searchProblems(query, count);
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