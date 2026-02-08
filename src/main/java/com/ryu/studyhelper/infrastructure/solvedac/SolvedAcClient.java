package com.ryu.studyhelper.infrastructure.solvedac;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.solvedac.client.SolvedAcRestClient;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserBioResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Component
@Slf4j
@RequiredArgsConstructor
public class SolvedAcClient {
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 30;
    private static final int MIN_SOLVED_COUNT = 1000;

    private final SolvedAcRestClient solvedAcRestClient;

    public SolvedAcUserResponse getUserInfo(String handle) {
        try {
            return solvedAcRestClient.getUserInfo(handle);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("solved.ac user not found: {}", handle);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to fetch user info from solved.ac: {}", handle, e);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_API_ERROR);
        }
    }

    /**
     * 풀지 않은 문제를 추천합니다.
     * @param handles 추천할 사용자 핸들 목록
     * @param count 추천할 문제 개수
     * @param minLevel 최소 난이도 (1~30, null이면 1)
     * @param maxLevel 최대 난이도 (1~30, null이면 30)
     * @param tagKeys 포함할 태그 키 목록 (null 또는 빈 목록이면 필터 없음)
     */
    public List<ProblemInfo> recommendUnsolvedProblems(List<String> handles, int count,
                                                       Integer minLevel, Integer maxLevel,
                                                       List<String> tagKeys) {
        try {
            String query = buildRecommendQuery(handles, minLevel, maxLevel, tagKeys);
            log.debug("solved.ac 추천 쿼리: {}", query);

            ProblemSearchResponse response = solvedAcRestClient.searchProblems(query, "random", "asc");
            if (response.items() == null) {
                return List.of();
            }

            return response.items().stream()
                    .map(ProblemInfo::withUrl)
                    .limit(count)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to recommend problems for handles: {}", handles, e);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_API_ERROR);
        }
    }

    private String buildRecommendQuery(List<String> handles, Integer minLevel, Integer maxLevel, List<String> tagKeys) {
        List<String> conditions = new ArrayList<>();

        // 난이도 범위
        int min = (minLevel != null && minLevel >= MIN_LEVEL && minLevel <= MAX_LEVEL) ? minLevel : MIN_LEVEL;
        int max = (maxLevel != null && maxLevel >= MIN_LEVEL && maxLevel <= MAX_LEVEL) ? maxLevel : MAX_LEVEL;
        conditions.add(String.format("*%d..%d", Math.min(min, max), Math.max(min, max)));

        // 기본 조건: 1000명 이상 풀이, 한국어
        conditions.add("s#" + MIN_SOLVED_COUNT + "..");
        conditions.add("lang:ko");

        // 태그 필터 (선택)
        if (tagKeys != null && !tagKeys.isEmpty()) {
            conditions.add(buildTagFilter(tagKeys));
        }

        // 사용자 제외 조건
        handles.forEach(h -> conditions.add("!s@" + h));

        return String.join("+", conditions);
    }

    private String buildTagFilter(List<String> tagKeys) {
        StringJoiner joiner = new StringJoiner("|", "(", ")");
        tagKeys.forEach(key -> joiner.add("tag:" + key));
        return joiner.toString();
    }

    /**
     * 백준 핸들 인증용 사용자 bio 조회
     */
    public SolvedAcUserBioResponse getUserBio(String handle) {
        try {
            return solvedAcRestClient.getUserBio(handle);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("solved.ac user not found: {}", handle);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to fetch user bio from solved.ac: {}", handle, e);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_API_ERROR);
        }
    }

    /**
     * 특정 사용자가 특정 문제를 풀었는지 확인
     */
    public boolean hasUserSolvedProblem(String handle, Long problemId) {
        try {
            String query = "id:" + problemId + "+s@" + handle;
            ProblemSearchResponse response = solvedAcRestClient.searchProblems(query, "id", "asc");
            return response.items() != null && !response.items().isEmpty();
        } catch (Exception e) {
            log.error("Failed to check if user {} solved problem {}", handle, problemId, e);
            throw new CustomException(CustomResponseStatus.SOLVED_AC_API_ERROR);
        }
    }
}