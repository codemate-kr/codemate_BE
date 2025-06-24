package com.ryu.studyhelper;

import com.ryu.studyhelper.dto.ProblemInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TestService {
    private final SolvedAcClient solvedAcClient;

    public TestService(SolvedAcClient solvedAcClient) {
        this.solvedAcClient = solvedAcClient;
    }

    public void printUserInfo(String handle) {
        try {
            SolvedAcUserResponse user = solvedAcClient.getUserInfo(handle);
            System.out.printf("🧑 %s | 티어: %d | 푼 문제 수: %d | 연속 %d일 | 레이팅: %d\n",
                    user.handle(), user.tier(), user.solvedCount(), user.maxStreak(), user.rating());
        } catch (Exception e) {
            System.err.println("에러 발생: " + e.getMessage());
        }
    }

    public List<ProblemInfo> recommend(List<String> handles, int count) {
        List<ProblemInfo> unsolvedProblems = new ArrayList<>();
        for(int i=0;i<500;i++){
            List<ProblemInfo> ret = solvedAcClient.getUnsolvedProblemsByUsers(handles, count);
            System.out.println(String.valueOf(i) +" "+ ret);
            unsolvedProblems.addAll(ret);
        }
        return unsolvedProblems;
    }

}
