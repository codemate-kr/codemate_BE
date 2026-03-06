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
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 추천 배치 오케스트레이션
 * - prepareDailyRecommendations: 매일 06:00 메인 배치
 * - retryFailed: 매일 07:00 PENDING/FAILED 재시도
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

    private record PendingEntry(Recommendation rec, Squad squad) {}

    public BatchResult prepareDailyRecommendations() {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        log.info("문제 추천 배치 시작: {}", missionDate);

        List<Squad> activeSquads = getActiveSquads(missionDate);
        if (activeSquads.isEmpty()) {
            log.info("오늘 추천 대상 스쿼드 없음");
            return new BatchResult(0, 0, 0, 0);
        }

        // 팀 지연 로딩 방지 — 한 번에 JOIN FETCH
        List<Long> squadIds = activeSquads.stream().map(Squad::getId).toList();
        Map<Long, Squad> squadWithTeam = squadRepository.findByIdsWithTeam(squadIds).stream()
                .collect(Collectors.toMap(Squad::getId, s -> s));

        // Phase 1: 핸들 체크 + PENDING INSERT
        int skipCount = 0;
        List<PendingEntry> pendingList = new ArrayList<>();

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
                pendingList.add(new PendingEntry(pending, squad));
            } catch (DataIntegrityViolationException e) {
                skipCount++;
                log.info("[{}] 스쿼드 '{}' PENDING 생성 스킵 — 이미 선점됨", squad.getTeam().getName(), squad.getName());
            }
        }

        // Phase 2: 순차 처리
        int successCount = 0, failCount = 0;
        for (PendingEntry entry : pendingList) {
            try {
                recommendationCreator.process(entry.rec(), entry.squad());
                successCount++;
            } catch (Exception e) {
                failCount++;
                Squad squad = entry.squad();
                log.error("[{}] 스쿼드 '{}' 추천 처리 실패", squad.getTeam().getName(), squad.getName(), e);
            }
        }

        log.info("문제 추천 배치 완료 — 대상: {}개, 성공: {}개, 스킵: {}개, 실패: {}개",
                activeSquads.size(), successCount, skipCount, failCount);
        return new BatchResult(activeSquads.size(), successCount, skipCount, failCount);
    }

    /**
     * 실패(status = FAILED) 미션 재시도 배치 작업.
     * FAILED 레코드를 PENDING으로 전이한 후 추천 생성을 다시 시도한다.
     * PENDING은 누군가 작업 중임을 의미하므로 재시도 대상에서 제외한다.
     */
    public BatchResult retryFailed() {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        List<Recommendation> targets = recommendationRepository.findByDateAndStatusIn(
                missionDate, List.of(RecommendationStatus.FAILED));

        log.info("재시도 대상: {}개 (date={})", targets.size(), missionDate);

        if (targets.isEmpty()) {
            return new BatchResult(0, 0, 0, 0);
        }

        List<Long> squadIds = targets.stream().map(Recommendation::getSquadId).toList();
        Map<Long, Squad> squadMap = squadRepository.findByIdsWithTeam(squadIds).stream()
                .collect(Collectors.toMap(Squad::getId, s -> s));

        int successCount = 0, failCount = 0, skipCount = 0;

        for (Recommendation rec : targets) {
            Squad squad = squadMap.get(rec.getSquadId());
            if (squad == null) {
                log.warn("스쿼드 ID {}를 찾을 수 없어 스킵합니다", rec.getSquadId());
                skipCount++;
                continue;
            }
            try {
                recommendationSaver.resetToPending(rec);
                recommendationCreator.process(rec, squad);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("[{}] 스쿼드 '{}' 재시도 실패", squad.getTeam().getName(), squad.getName(), e);
            }
        }

        log.info("재시도 완료 — 대상: {}개, 성공: {}개, 스킵: {}개, 실패: {}개",
                targets.size(), successCount, skipCount, failCount);
        return new BatchResult(targets.size(), successCount, skipCount, failCount);
    }

    private List<Squad> getActiveSquads(LocalDate date) {
        int dayBit = RecommendationDayOfWeek.from(date.getDayOfWeek()).getBitValue();
        return squadRepository.findActiveSquadsForDay(dayBit);
    }
}
