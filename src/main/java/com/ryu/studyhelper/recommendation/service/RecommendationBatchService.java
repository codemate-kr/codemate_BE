package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.concurrent.Executor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 추천 배치 오케스트레이션
 * - prepareDailyRecommendations: 매일 06:00 메인 배치
 * - retryFailed: 매일 07:00 FAILED 재시도
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationBatchService {

    private final Clock clock;
    private final SquadRepository squadRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationSaver recommendationSaver;
    private final RecommendationCreator recommendationCreator;
    private final Executor recommendationBatchExecutor;

    private record PendingRecommendation(Recommendation rec, Squad squad) {}

    // ── 메인 배치 ──────────────────────────────────────────────────────────────

    public BatchResult prepareDailyRecommendations() {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        log.info("문제 추천 배치 시작: {}", missionDate);

        List<Squad> activeSquads = findActiveSquads(missionDate);
        if (activeSquads.isEmpty()) {
            log.info("오늘 추천 대상 스쿼드 없음");
            return new BatchResult(0, 0, 0, 0);
        }

        // 팀 지연 로딩 방지 — 한 번에 JOIN FETCH
        List<Long> squadIds = activeSquads.stream().map(Squad::getId).toList();
        Map<Long, Squad> squadWithTeam = loadSquadsWithTeam(squadIds);

        // Phase 1: 핸들 체크 + PENDING INSERT
        int skipCount = 0;
        List<PendingRecommendation> pendingList = new ArrayList<>();

        for (Squad rawSquad : activeSquads) {
            Squad squad = squadWithTeam.get(rawSquad.getId());
            if (squad == null || squad.getTeam() == null) {
                skipCount++;
                log.warn("스쿼드 ID {}의 팀 정보를 찾을 수 없어 스킵합니다", rawSquad.getId());
                continue;
            }
            Long teamId = squad.getTeam().getId();
            List<String> handles = teamMemberRepository.findHandlesByTeamIdAndSquadId(teamId, squad.getId());
            if (handles.isEmpty()) {
                skipCount++;
                log.info("[{}] 스쿼드 '{}' 스킵 — 인증된 핸들 없음", squad.getTeam().getName(), squad.getName());
                continue;
            }
            try {
                Recommendation pending = recommendationSaver.createPending(squad, missionDate, RecommendationType.SCHEDULED);
                pendingList.add(new PendingRecommendation(pending, squad));
            } catch (DataIntegrityViolationException e) {
                skipCount++;
                log.info("[{}] 스쿼드 '{}' PENDING 생성 스킵 — 이미 선점됨", squad.getTeam().getName(), squad.getName());
            }
        }

        // Phase 2: 병렬 처리
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        processAll(pendingList, successCount, failCount);

        log.info("문제 추천 배치 완료 — 대상: {}개, 성공: {}개, 스킵: {}개, 실패: {}개",
                activeSquads.size(), successCount.get(), skipCount, failCount.get());
        return new BatchResult(activeSquads.size(), successCount.get(), skipCount, failCount.get());
    }

    private List<Squad> findActiveSquads(LocalDate date) {
        int dayBit = RecommendationDayOfWeek.from(date.getDayOfWeek()).getBitValue();
        return squadRepository.findActiveSquadsForDay(dayBit);
    }

    private Map<Long, Squad> loadSquadsWithTeam(List<Long> squadIds) {
        return squadRepository.findByIdsWithTeam(squadIds).stream()
                .collect(Collectors.toMap(Squad::getId, s -> s));
    }

    // Phase 2: 스쿼드당 SolvedAC HTTP 호출이 병목 → recommendationBatchExecutor로 동시 처리
    private void processAll(List<PendingRecommendation> pendingList, AtomicInteger successCount, AtomicInteger failCount) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(pendingList.size());
        for (PendingRecommendation entry : pendingList) {
            try {
                futures.add(CompletableFuture.runAsync(
                        () -> processSquad(entry, successCount, failCount),
                        recommendationBatchExecutor));
            } catch (RejectedExecutionException e) {
                failCount.incrementAndGet();
                log.error("[{}] 스쿼드 '{}' 작업 제출 실패 — Executor 포화",
                        entry.squad().getTeam().getName(), entry.squad().getName(), e);
            }
        }
        futures.forEach(CompletableFuture::join);
    }

    private void processSquad(PendingRecommendation entry, AtomicInteger successCount, AtomicInteger failCount) {
        try {
            recommendationCreator.process(entry.rec(), entry.squad());
            successCount.incrementAndGet();
            log.debug("[{}] 스쿼드 '{}' 추천 처리 완료", entry.squad().getTeam().getName(), entry.squad().getName());
        } catch (Exception e) {
            failCount.incrementAndGet();
            log.error("[{}] 스쿼드 '{}' 추천 처리 실패", entry.squad().getTeam().getName(), entry.squad().getName(), e);
        }
    }

    // ── 재시도 배치 ────────────────────────────────────────────────────────────

    /**
     * FAILED 미션 재시도 배치.
     * 수동 추천 API(createManual)와 동시 실행될 수 있어 FAILED → PENDING CAS로 선점 후 처리한다.
     * 이메일 재시도(RecommendationEmailService.retryFailed)도 동일한 패턴을 사용한다.
     */
    public BatchResult retryFailed() {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        List<Recommendation> failedRecommendations = recommendationRepository.findByDateAndStatusIn(
                missionDate, List.of(RecommendationStatus.FAILED));

        log.info("재시도 대상: {}개 (date={})", failedRecommendations.size(), missionDate);
        if (failedRecommendations.isEmpty()) {
            return new BatchResult(0, 0, 0, 0);
        }

        List<Long> squadIds = failedRecommendations.stream().map(Recommendation::getSquadId).toList();
        Map<Long, Squad> squadWithTeam = loadSquadsWithTeam(squadIds);

        List<PendingRecommendation> claimedList = new ArrayList<>();
        int skipCount = 0, preemptedCount = 0;

        for (Recommendation recommendation : failedRecommendations) {
            Squad squad = squadWithTeam.get(recommendation.getSquadId());
            if (squad == null) {
                log.warn("스쿼드 ID {}를 찾을 수 없어 스킵합니다", recommendation.getSquadId());
                skipCount++;
                continue;
            }
            if (!recommendationSaver.tryPrepareForRetry(recommendation)) {
                log.info("[{}] 스쿼드 '{}' 재시도 스킵 — 다른 워커가 선점함", squad.getTeam().getName(), squad.getName());
                preemptedCount++; // 다른 워커가 선점 — 이 워커의 처리 대상에서 제외
                continue;
            }
            claimedList.add(new PendingRecommendation(recommendation, squad));
        }

        int successCount = 0, failCount = 0;
        for (PendingRecommendation entry : claimedList) {
            try {
                recommendationCreator.process(entry.rec(), entry.squad());
                successCount++;
            } catch (Exception e) {
                failCount++;
                Squad squad = entry.squad();
                log.error("[{}] 스쿼드 '{}' 재시도 처리 실패", squad.getTeam().getName(), squad.getName(), e);
            }
        }

        int totalCount = failedRecommendations.size() - preemptedCount;
        log.info("재시도 완료 — 대상: {}개, 성공: {}개, 스킵: {}개, 실패: {}개",
                totalCount, successCount, skipCount, failCount);
        return new BatchResult(totalCount, successCount, skipCount, failCount);
    }
}
