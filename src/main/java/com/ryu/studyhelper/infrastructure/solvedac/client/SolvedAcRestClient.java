package com.ryu.studyhelper.infrastructure.solvedac.client;

import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserBioResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class SolvedAcRestClient {
    private final RestClient rest;

    public SolvedAcRestClient() {
        this.rest = RestClient.builder()
                .baseUrl("https://solved.ac/api/v3")
                .defaultHeader("User-Agent", "studyhelper/1.0")
                .build();
    }



    private <T> T get(String path, Map<String, String> params, Class<T> responseType) {
        return rest.get()
                .uri(b -> {
                    var ub = b.path(path);
                    if (params != null) params.forEach(ub::queryParam);
                    return ub.build(); // 인코딩/결합은 RestClient에 맡김
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(responseType);
    }


    public SolvedAcUserResponse getUserInfo(String handle) {
        return get("/user/show", Map.of("handle", handle), SolvedAcUserResponse.class);
    }


    /**
     * 문제 검색 API 호출 (순수 HTTP 호출만 담당)
     * @param query 검색 쿼리
     * @param sort 정렬 기준 (id, level, title, solved, random 등)
     * @param direction 정렬 방향 (asc, desc)
     * @return API 응답 원본
     */
    public ProblemSearchResponse searchProblems(String query, String sort, String direction) {
        return get("/search/problem", Map.of(
                "query", query,
                "sort", sort,
                "direction", direction
        ), ProblemSearchResponse.class);
    }

    /**
     * 백준 핸들 인증용 사용자 정보 조회 (bio 포함)
     * @param handle 백준 핸들
     * @return 핸들과 bio 정보
     */
    public SolvedAcUserBioResponse getUserBio(String handle) {
        return get("/user/show", Map.of("handle", handle), SolvedAcUserBioResponse.class);
    }

}