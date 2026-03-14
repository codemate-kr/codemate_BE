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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
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

    private enum FailMode {
        NONE,         // 정상 동작 (지연 시뮬레이션)
        NETWORK_ERROR, // ResourceAccessException — 타임아웃/네트워크 장애
        SERVER_ERROR   // HttpServerErrorException(502) — solved.ac 서버 장애
    }

    private static final long DELAY_MS = 5_000;
    private static final FailMode FAIL_MODE = FailMode.NONE;

    @Retry(name = "solvedAc")
    @CircuitBreaker(name = "solvedAc")
    @Override
    public SolvedAcUserResponse getUserInfo(String handle) {
        simulate("getUserInfo", handle);
        SolvedAcUserResponse response = new SolvedAcUserResponse(handle, 15, 200, 100, 1500);
        log.debug("[FAKE] getUserInfo 반환\n  handle: {}, tier: {}, solvedCount: {}, rating: {}",
                response.handle(), response.tier(), response.solvedCount(), response.rating());
        return response;
    }

    @Retry(name = "solvedAc")
    @CircuitBreaker(name = "solvedAc")
    @Override
    public ProblemSearchResponse searchProblems(String query, String sort, String direction) {
        simulate("searchProblems", query);
        ProblemSearchResponse response = new ProblemSearchResponse(List.of(
                createDummyProblem(1000L, "A+B", 1),
                createDummyProblem(1001L, "A-B", 1),
                createDummyProblem(2557L, "Hello World", 2),
                createDummyProblem(10828L, "스택", 7),
                createDummyProblem(1149L, "RGB거리", 10)
        ));
        String problemSummary = response.items().stream()
                .map(p -> p.problemId() + "(" + p.titleKo() + ", lv." + p.level() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("(없음)");
        log.debug("[FAKE] searchProblems 반환\n  query: {}\n  problems: {}", query, problemSummary);
        return response;
    }

    @Retry(name = "solvedAc")
    @CircuitBreaker(name = "solvedAc")
    @Override
    public SolvedAcUserBioResponse getUserBio(String handle) {
        simulate("getUserBio", handle);
        SolvedAcUserBioResponse response = new SolvedAcUserBioResponse(handle, "fake bio for testing");
        log.debug("[FAKE] getUserBio 반환\n  handle: {}, bio: {}", response.handle(), response.bio());
        return response;
    }

    private void simulate(String method, String param) {
        if (FAIL_MODE == FailMode.NETWORK_ERROR) {
            log.debug("[FAKE] {} 네트워크 에러 시뮬레이션 - param: {}", method, param);
            throw new ResourceAccessException("[FAKE] SolvedAC 네트워크 에러");
        }
        if (FAIL_MODE == FailMode.SERVER_ERROR) {
            log.debug("[FAKE] {} 서버 에러 시뮬레이션 (502) - param: {}", method, param);
            throw new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "[FAKE] SolvedAC 502 Bad Gateway");
        }
        log.debug("[FAKE] {} 호출 - param: {}, {}ms 지연 시뮬레이션", method, param, DELAY_MS);
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