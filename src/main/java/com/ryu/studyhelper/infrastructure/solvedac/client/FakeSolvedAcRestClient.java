package com.ryu.studyhelper.infrastructure.solvedac.client;

import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemSearchResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcTagInfo;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserBioResponse;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcUserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 로컬 테스트용 SolvedAcHttpClient
 * 실제 API 호출 대신 지연 + 더미 데이터 반환
 */
@Component
@Profile("local")
@Primary
@Slf4j
public class FakeSolvedAcRestClient implements SolvedAcHttpClient {

    private static final long DELAY_MS = 20_000;
    private static final boolean FAIL_IMMEDIATELY = false; // true → 즉시 RuntimeException (failure-rate-threshold 트리거)

    @Retry(name = "solvedAc")
    @CircuitBreaker(name = "solvedAc")
    @Override
    public SolvedAcUserResponse getUserInfo(String handle) {
        simulate("getUserInfo", handle);
        return new SolvedAcUserResponse(handle, 15, 200, 100, 1500);
    }

    @Retry(name = "solvedAc")
    @CircuitBreaker(name = "solvedAc")
    @Override
    public ProblemSearchResponse searchProblems(String query, String sort, String direction) {
        simulate("searchProblems", query);
        return new ProblemSearchResponse(List.of(
                createDummyProblem(1000L, "A+B", 1),
                createDummyProblem(1001L, "A-B", 1),
                createDummyProblem(2557L, "Hello World", 2),
                createDummyProblem(10828L, "스택", 7),
                createDummyProblem(1149L, "RGB거리", 10)
        ));
    }

    @Retry(name = "solvedAc")
    @CircuitBreaker(name = "solvedAc")
    @Override
    public SolvedAcUserBioResponse getUserBio(String handle) {
        simulate("getUserBio", handle);
        return new SolvedAcUserBioResponse(handle, "fake bio for testing");
    }

    private void simulate(String method, String param) {
        if (FAIL_IMMEDIATELY) {
            log.debug("[FAKE] {} 즉시 에러 시뮬레이션 - param: {}", method, param);
            throw new ResourceAccessException("[FAKE] SolvedAC 즉시 에러");
        }
        log.debug("[FAKE] {} {}ms 지연 시뮬레이션 - param: {}", method, DELAY_MS, param);
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ProblemInfo createDummyProblem(Long id, String title, int level) {
        return new ProblemInfo(
                id, title, level, 50000, 1.5,
                "https://www.acmicpc.net/problem/" + id,
                List.of(new SolvedAcTagInfo(
                        "implementation", false, 1,
                        List.of(
                                new SolvedAcTagInfo.DisplayName("ko", "구현", "구현"),
                                new SolvedAcTagInfo.DisplayName("en", "implementation", "implementation")
                        )
                ))
        );
    }
}