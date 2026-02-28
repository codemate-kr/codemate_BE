package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 배치 문제 추천 오케스트레이션
 * 매일 새벽 6시 스케줄러가 호출
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ScheduledRecommendationService {

    private final Clock clock;
    private final SquadRepository squadRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationCreator recommendationCreator;

    /**
     * 문제 추천만 수행 (이메일 발송 X)
     * 미션 사이클 기준(06:00~06:00)으로 중복 체크
     */
    public BatchResult prepareDailyRecommendations() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);
        log.info("문제 추천 준비 시작: {} (미션 사이클: {} 06:00 ~)", now.toLocalDate(), missionCycleStart.toLocalDate());

        List<Squad> activeSquads = getActiveSquads(now.toLocalDate());
        int successCount = 0;
        int failCount = 0;

        for (Squad squad : activeSquads) {
            try {
                if (recommendationRepository.findFirstByTeamIdAndSquadIdAndCreatedAtBetweenOrderById(
                        squad.getTeam().getId(), squad.getId(), missionCycleStart, now).isPresent()) {
                    log.info("[{}] 스쿼드 '{}'에 이미 추천 존재 - 스킵", squad.getTeam().getName(), squad.getName());
                    continue;
                }
                recommendationCreator.createForSquad(squad, RecommendationType.SCHEDULED);
                successCount++;
                log.info("[{}] 스쿼드 '{}' 문제 추천 완료", squad.getTeam().getName(), squad.getName());
            } catch (Exception e) {
                failCount++;
                log.error("[{}] 스쿼드 '{}' 문제 추천 실패", squad.getTeam().getName(), squad.getName(), e);
            }
        }

        log.info("문제 추천 배치 완료 - 대상: {}개, 성공: {}개, 실패: {}개",
                activeSquads.size(), successCount, failCount);
        return new BatchResult(activeSquads.size(), successCount, failCount);
    }

    private List<Squad> getActiveSquads(LocalDate date) {
        int dayBit = RecommendationDayOfWeek.from(date.getDayOfWeek()).getBitValue();
        return squadRepository.findActiveSquadsForDay(dayBit);
    }
}
