package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.service.SquadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
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
    private final TeamRepository teamRepository;
    private final SquadRepository squadRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationCreator recommendationCreator;
    private final SquadService squadService;

    /**
     * 문제 추천만 수행 (이메일 발송 X)
     * 미션 사이클 기준(06:00~06:00)으로 중복 체크
     */
    public BatchResult prepareDailyRecommendations() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);
        log.info("문제 추천 준비 시작: {} (미션 사이클: {} 06:00 ~)", now.toLocalDate(), missionCycleStart.toLocalDate());

        List<Team> activeTeams = getActiveTeams(now.toLocalDate());
        int successCount = 0;
        int failCount = 0;

        // TODO(#172): 2차 배포 시 루프 전체 교체 — 아래 스쿼드 기반으로 변경
        //   List<Squad> activeSquads = getActiveSquads(now.toLocalDate());
        //   for (Squad squad : activeSquads) {
        //       if (recommendationRepository.findFirstByTeamIdAndSquadIdAndCreatedAtBetweenOrderById(
        //               squad.getTeam().getId(), squad.getId(), missionCycleStart, now).isPresent()) { continue; }
        //       recommendationCreator.createForSquad(squad, RecommendationType.SCHEDULED);
        //   }
        //   getActiveTeams(), squadService.findDefaultSquad() 호출 제거
        for (Team team : activeTeams) {
            try {
                if (recommendationRepository.findFirstByTeamIdAndCreatedAtBetweenOrderById(
                        team.getId(), missionCycleStart, now
                ).isPresent()) {
                    log.debug("팀 '{}'에 대해 현재 미션 사이클({})에 이미 추천 존재 - 스킵", team.getName(), missionCycleStart);
                    continue;
                }

                Squad defaultSquad = squadService.findDefaultSquad(team.getId());
                recommendationCreator.createForSquad(defaultSquad, RecommendationType.SCHEDULED);
                successCount++;
                log.info("팀 '{}' 문제 추천 완료", team.getName());

            } catch (Exception e) {
                failCount++;
                log.error("팀 '{}' 문제 추천 실패", team.getName(), e);
            }
        }

        log.info("문제 추천 배치 완료 - 대상: {}개, 성공: {}개, 실패: {}개",
                activeTeams.size(), successCount, failCount);
        return new BatchResult(activeTeams.size(), successCount, failCount);
    }

    // TODO(#172): 2차 배포 시 제거 - getActiveSquads()로 교체, TeamRepository.findAll() 제거
    private List<Team> getActiveTeams(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return teamRepository.findAll().stream()
                .filter(team -> !team.getTeamMembers().isEmpty())
                .filter(Team::isRecommendationActive)
                .filter(team -> team.isRecommendationDay(dayOfWeek))
                .toList();
    }

    /**
     * 2차 배포 시 getActiveTeams() 대체 메서드
     * recommendation_status=ACTIVE이고 해당 요일이 설정된 스쿼드 목록을 DB에서 직접 조회
     */
    private List<Squad> getActiveSquads(LocalDate date) {
        int dayBit = RecommendationDayOfWeek.from(date.getDayOfWeek()).getBitValue();
        return squadRepository.findActiveSquadsForDay(dayBit);
    }
}
