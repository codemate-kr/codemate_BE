package com.ryu.studyhelper.solvedac.api;

import com.ryu.studyhelper.solvedac.dto.BojVerificationDto;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.solvedac.dto.SolvedAcUserResponse;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import java.time.Duration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import java.util.Map;

@Component
public class SolvedAcClient {
    private final RestClient rest;

    public SolvedAcClient() {
//        PoolingHttpClientConnectionManager pool = PoolingHttpClientConnectionManagerBuilder.create()
//                .setMaxConnTotal(200)
//                .setMaxConnPerRoute(50)
//                .build();
//        CloseableHttpClient http = HttpClients.custom()
//                .setConnectionManager(pool)
//                .evictExpiredConnections()
//                .build();
//        HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(http);
//        rf.setConnectTimeout(Duration.ofSeconds(5));
//        rf.setReadTimeout(Duration.ofSeconds(5));

        this.rest = RestClient.builder()
//                .requestFactory(rf)
                .baseUrl("https://solved.ac/api/v3")
                .defaultHeader("User-Agent", "studyhelper/1.0")
                .build();
    }



    private <T> T get(String path, Map<String, String> params, Class<T> ResponseType) {
        return rest.get()
                .uri(b -> {
                    var ub = b.path(path);
                    if (params != null) params.forEach(ub::queryParam);
                    return ub.build(); // 인코딩/결합은 RestClient에 맡김
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ResponseType);
    }


    public SolvedAcUserResponse getUserInfo(String handle) {
        return get("/user/show", Map.of("handle", handle), SolvedAcUserResponse.class);
    }


    public ProblemSearchResponse searchProblems(String query, int count) {
        ProblemSearchResponse resp = get("/search/problem", Map.of(
                "query", query,
                "sort", "random",
                "direction", "asc"
        ), ProblemSearchResponse.class);

        //count개 만 리턴
        var limited = resp.items().stream()
                .map(ProblemInfo::withUrl)
                .limit(count)
                .toList();

        return new ProblemSearchResponse(limited);
    }



    public ProblemSearchResponse getSolvedProblemsRaw(String handle) {
        return get("/search/problem", Map.of(
                "query", "solved_by:" + handle,
                "sort", "accuracy",
                "direction", "desc"
        ), ProblemSearchResponse.class);
    }

    /**
     * 백준 핸들 인증용 사용자 정보 조회 (bio 포함)
     * @param handle 백준 핸들
     * @return 핸들과 bio 정보
     */
    public BojVerificationDto getUserBio(String handle) {
        return get("/user/show", Map.of("handle", handle), BojVerificationDto.class);
    }

}