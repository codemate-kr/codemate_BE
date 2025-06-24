package com.ryu.studyhelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.ryu.studyhelper.dto.ProblemInfo;
import com.ryu.studyhelper.dto.ProblemSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SolvedAcClient {

    private final WebClient webClient;

    public SolvedAcClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://solved.ac/api/v3").build();
    }



    public SolvedAcUserResponse getUserInfo(String handle) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/show")
                        .queryParam("handle", handle)
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String msg = "[solved.ac] API 호출 실패: " + body;
                                    return Mono.error(new RuntimeException(msg));
                                }))
                .bodyToMono(SolvedAcUserResponse.class)
                .block(); // 블로킹 호출 (동기화)
    }




    public List<ProblemInfo> getUnsolvedProblemsByUsers(List<String> handles, int count) {
        String handleQuery = handles.stream()
                .map(h -> "!s@" + h)
                .collect(Collectors.joining("+"));

        String query = "*g5..g1+s#1000..+lang:ko+" + handleQuery;

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