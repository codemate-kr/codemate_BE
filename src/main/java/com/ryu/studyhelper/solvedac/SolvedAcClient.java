package com.ryu.studyhelper.solvedac;

import com.ryu.studyhelper.dto.ProblemInfo;
import com.ryu.studyhelper.dto.ProblemSearchResponse;
import com.ryu.studyhelper.solvedac.dto.SolvedAcUserResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class SolvedAcClient {

    private final WebClient webClient;

    public SolvedAcClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://solved.ac/api/v3").build();
    }



    public SolvedAcUserResponse getUserInfo(String handle) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user/show").queryParam("handle", handle).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        resp -> resp.bodyToMono(String.class).flatMap(msg -> Mono.error(new RuntimeException("[solved.ac] 실패: " + msg))))
                .bodyToMono(SolvedAcUserResponse.class)
                .block();
    }

    public List<ProblemInfo> searchProblems(String query, int count) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/problem")
                        .queryParam("query", query)
                        .queryParam("sort", "random")
                        .queryParam("direction", "asc")
                        .build())
                .retrieve()
                .bodyToMono(ProblemSearchResponse.class)
                .map(resp -> resp.items().stream()
                        .limit(count)
                        .map(ProblemInfo::withUrl)
                        .toList())
                .block();
    }
}