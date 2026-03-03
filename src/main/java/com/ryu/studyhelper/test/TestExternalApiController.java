package com.ryu.studyhelper.test;

import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외부 API 지연이 서비스에 미치는 영향을 테스트하는 컨트롤러
 *
 * 시나리오 1: /with-tx → @Transactional + SolvedAC 호출 → DB 커넥션 풀 고갈 (10건이면 먹통)
 * 시나리오 2: /without-tx → SolvedAC 호출만 → Tomcat 스레드 풀 고갈 (200건이면 먹통)
 */
@Slf4j
@RestController
@RequestMapping("/api/test/external-api")
@Profile("local")
@RequiredArgsConstructor
@Tag(name = "Test - External API", description = "외부 API 지연 영향 테스트 (local 환경 전용)")
public class TestExternalApiController {

    private final SolvedAcClient solvedAcClient;
    private final MemberRepository memberRepository;

    @GetMapping("/with-tx")
    @Transactional
    @Operation(summary = "트랜잭션 + SolvedAC 호출", description = "DB 커넥션을 잡은 채 SolvedAC 대기. 10건 동시 호출 시 커넥션 풀 고갈.")
    public String withTransaction() {
        long threadId = Thread.currentThread().getId();
        log.debug("[with-tx] 요청 시작 (thread-{})", threadId);

        // DB 커넥션 획득 (쿼리 실행)
        long count = memberRepository.count();
        log.debug("[with-tx] DB 쿼리 완료, 회원 수: {} (thread-{})", count, threadId);

        // SolvedAC 호출 (커넥션 점유 중)
        solvedAcClient.getUserInfo("testhandle");

        log.debug("[with-tx] 요청 완료 (thread-{})", threadId);
        return "done (with-tx, thread-" + threadId + ")";
    }

    @GetMapping("/without-tx")
    @Operation(summary = "트랜잭션 없이 SolvedAC 호출", description = "DB 커넥션 없이 SolvedAC 대기. 200건 동시 호출 시 Tomcat 스레드 풀 고갈.")
    public String withoutTransaction() {
        long threadId = Thread.currentThread().getId();
        log.debug("[without-tx] 요청 시작 (thread-{})", threadId);

        // SolvedAC 호출 (DB 커넥션 없음, Tomcat 스레드만 점유)
        solvedAcClient.getUserInfo("testhandle");

        log.debug("[without-tx] 요청 완료 (thread-{})", threadId);
        return "done (without-tx, thread-" + threadId + ")";
    }

    @GetMapping("/health")
    @Operation(summary = "헬스체크 (DB 없음)", description = "DB 접근 없이 즉시 응답. 서비스 응답 가능 여부 확인용.")
    public String health() {
        return "OK";
    }

    @GetMapping("/db-health")
    @Transactional(readOnly = true)
    @Operation(summary = "헬스체크 (DB 사용)", description = "DB 쿼리 실행. 커넥션 풀 고갈 시 응답 불가.")
    public String dbHealth() {
        long count = memberRepository.count();
        return "OK (members: " + count + ")";
    }
}