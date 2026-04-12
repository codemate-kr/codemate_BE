package com.ryu.studyhelper.infrastructure.solvedac.client;

import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserBioResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.http.MediaType;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation") // OkHttp3ClientHttpRequestFactory: Spring 6.1 deprecated, Spring 7 제거 예정. JDK HttpClient는 Cloudflare TLS 지문(JA3) 차단으로 사용 불가.
@Component
public class SolvedAcRestClient implements SolvedAcHttpClient {
    private static final String DEFAULT_BASE_URL = "https://solved.ac/api/v3";
    private static final int CONNECT_TIMEOUT_SECONDS = 3;       // TCP 연결 타임아웃
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;     // 서버 응답 타임아웃 (최후 안전망, 지연 감지는 서킷브레이커 담당)

    private final RestClient rest;

    public SolvedAcRestClient() {
        this.rest = RestClient.builder()
                .baseUrl(DEFAULT_BASE_URL)
                .defaultHeader("User-Agent", "codemate/1.0")
                .requestFactory(buildRequestFactory())
                .build();
    }

    private OkHttp3ClientHttpRequestFactory buildRequestFactory() {
        // OkHttp: HTTP/2 우선 협상(ALPN), HTTP/1.1 폴백, 커넥션 풀 자동 관리
        // Android/Chrome 유사 TLS 지문(JA3)으로 Cloudflare managed challenge 통과
        var okHttpClient = new OkHttpClient.Builder()
                .protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        return new OkHttp3ClientHttpRequestFactory(okHttpClient);
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


    @Retry(name = "solvedAc")
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
    @Retry(name = "solvedAc")
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
    @Retry(name = "solvedAc")
    @CircuitBreaker(name = "solvedAc")
    public SolvedAcUserBioResponse getUserBio(String handle) {
        return get("/user/show", Map.of("handle", handle), SolvedAcUserBioResponse.class);
    }

}
