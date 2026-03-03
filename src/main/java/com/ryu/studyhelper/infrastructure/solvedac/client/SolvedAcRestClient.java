package com.ryu.studyhelper.infrastructure.solvedac.client;

import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserBioResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class SolvedAcRestClient {
    private static final String DEFAULT_BASE_URL = "https://solved.ac/api/v3";
    private static final int CONNECT_TIMEOUT_SECONDS = 3;           // TCP 연결 타임아웃
    private static final int CONN_REQUEST_TIMEOUT_SECONDS = 3;      // 풀에서 커넥션 대기 타임아웃 (solved.ac 평균 응답 ~1s 고려)
    private static final int RESPONSE_TIMEOUT_SECONDS = 10;         // 서버 응답 타임아웃
    private static final int MAX_CONN_PER_ROUTE = 5;                // 비동기 전환 시 동시 실행 수와 맞출 것
    private static final int MAX_CONN_TOTAL = 10;

    private final RestClient rest;

    public SolvedAcRestClient() {
        this.rest = RestClient.builder()
                .baseUrl(DEFAULT_BASE_URL)
                .defaultHeader("User-Agent", "codemate/1.0")
                .requestFactory(buildRequestFactory())
                .build();
    }

    private HttpComponentsClientHttpRequestFactory buildRequestFactory() {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                .setMaxConnTotal(MAX_CONN_TOTAL)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                        .build())
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(CONN_REQUEST_TIMEOUT_SECONDS))
                        .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                        .build())
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
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


    @CircuitBreaker(name = "solvedAc")
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
    @CircuitBreaker(name = "solvedAc")
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
    @CircuitBreaker(name = "solvedAc")
    public SolvedAcUserBioResponse getUserBio(String handle) {
        return get("/user/show", Map.of("handle", handle), SolvedAcUserBioResponse.class);
    }

}
