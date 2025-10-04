package com.ryu.studyhelper.solvedac;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.solvedac.api.SolvedAcClient;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.solvedac.dto.SolvedAcUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class SolvedAcService {
    private final SolvedAcClient solvedAcClient;

    public SolvedAcService(SolvedAcClient solvedAcClient) {
        this.solvedAcClient = solvedAcClient;
    }

    public SolvedAcUserResponse getUserInfo(String handle) {
        try {
            return solvedAcClient.getUserInfo(handle);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("solved.ac user not found: {}", handle);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to fetch user info from solved.ac: {}", handle, e);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_API_ERROR);
        }
    }

    /**
     * 주어진 사용자 핸들에 대해, 1000명이상이 푼 골드문제중 풀지 않은 문제를 추천합니다.
     * @param handles 추천할 사용자 핸들 목록
     * @param count 추천할 문제 개수
     * @return 추천된 문제 목록
     */
    public List<ProblemInfo> recommendUnsolvedProblems(List<String> handles, int count) {
        return recommendUnsolvedProblems(handles, count, null, null);
    }

    /**
     * 주어진 사용자 핸들과 난이도 범위에 대해 풀지 않은 문제를 추천합니다.
     * @param handles 추천할 사용자 핸들 목록
     * @param count 추천할 문제 개수
     * @param minLevel 최소 난이도 (1~30, null이면 기본값 적용)
     * @param maxLevel 최대 난이도 (1~30, null이면 기본값 적용)
     * @return 추천된 문제 목록
     */
    public List<ProblemInfo> recommendUnsolvedProblems(List<String> handles, int count, Integer minLevel, Integer maxLevel) {
        // 난이도 범위 설정 (기본값: 골드 5 ~ 골드 1)
        String levelRange = buildLevelRange(minLevel, maxLevel);

        String query = Stream.concat(
                        Stream.of(levelRange, "s#1000..", "lang:ko"),
                        handles.stream().map(h -> "!s@" + h))
                .collect(Collectors.joining("+"));

        ProblemSearchResponse response = solvedAcClient.searchProblems(query, count);
        return response.items().stream()
                .map(ProblemInfo::withUrl)
                .toList();
    }

    /**
     * 난이도 범위 쿼리 문자열 생성
     */
    private String buildLevelRange(Integer minLevel, Integer maxLevel) {
        // 기본값: 골드 5(11) ~ 골드 1(15)
        int min = (minLevel != null && minLevel >= 1 && minLevel <= 30) ? minLevel : 11;
        int max = (maxLevel != null && maxLevel >= 1 && maxLevel <= 30) ? maxLevel : 15;

        // 범위 검증
        if (min > max) {
            min = 11;
            max = 15;
        }

        return String.format("*%d..%d", min, max);
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