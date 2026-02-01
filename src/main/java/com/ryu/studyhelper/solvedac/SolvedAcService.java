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
        return recommendUnsolvedProblems(handles, count, minLevel, maxLevel, null);
    }

    /**
     * 주어진 사용자 핸들, 난이도 범위, 태그 필터에 대해 풀지 않은 문제를 추천합니다.
     * @param handles 추천할 사용자 핸들 목록
     * @param count 추천할 문제 개수
     * @param minLevel 최소 난이도 (1~30, null이면 기본값 적용)
     * @param maxLevel 최대 난이도 (1~30, null이면 기본값 적용)
     * @param tagKeys 포함할 태그 키 목록 (null 또는 빈 목록이면 필터 없음)
     * @return 추천된 문제 목록
     */
    public List<ProblemInfo> recommendUnsolvedProblems(List<String> handles, int count, Integer minLevel, Integer maxLevel, List<String> tagKeys) {
        // 난이도 범위 설정 (기본값: 골드 5 ~ 골드 1)
        String levelRange = buildLevelRange(minLevel, maxLevel);

        // 태그 필터 생성 (예: "(tag:dp|tag:greedy)")
        String tagFilter = buildTagFilter(tagKeys);

        // 쿼리 조합
        Stream<String> baseQuery = Stream.of(levelRange, "s#1000..", "lang:ko");
        Stream<String> userExclusions = handles.stream().map(h -> "!s@" + h);

        String query;
        if (tagFilter != null) {
            query = Stream.concat(
                    Stream.concat(baseQuery, Stream.of(tagFilter)),
                    userExclusions
            ).collect(Collectors.joining("+"));
        } else {
            query = Stream.concat(baseQuery, userExclusions)
                    .collect(Collectors.joining("+"));
        }

        log.debug("solved.ac 추천 쿼리: {}", query);
        ProblemSearchResponse response = solvedAcClient.searchProblems(query, "random", "asc");
        return response.items().stream()
                .map(ProblemInfo::withUrl)
                .limit(count)  // Service에서 개수 제한
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

    /**
     * 태그 필터 쿼리 문자열 생성
     * 예: ["dp", "greedy"] → "(tag:dp|tag:greedy)"
     * @param tagKeys 태그 키 목록
     * @return 태그 필터 문자열 (빈 목록이면 null)
     */
    private String buildTagFilter(List<String> tagKeys) {
        if (tagKeys == null || tagKeys.isEmpty()) {
            return null;
        }

        String tagConditions = tagKeys.stream()
                .map(key -> "tag:" + key)
                .collect(Collectors.joining("|"));

        return "(" + tagConditions + ")";
    }

    /**
     * 특정 사용자가 특정 문제를 풀었는지 확인
     * @param handle 사용자 핸들
     * @param problemId 문제 번호
     * @return 해결 여부
     */
    public boolean hasUserSolvedProblem(String handle, Long problemId) {
        try {
            // 쿼리 조합: 문제 ID와 사용자 해결 여부 조건
            String query = "id:" + problemId + " s@" + handle;
            ProblemSearchResponse response = solvedAcClient.searchProblems(query, "id", "asc");
            // 결과 판단: 검색 결과가 있으면 해결한 것
            return response.items() != null && !response.items().isEmpty();
        } catch (Exception e) {
            log.error("Failed to check if user {} solved problem {}", handle, problemId, e);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_API_ERROR);
        }
    }



}