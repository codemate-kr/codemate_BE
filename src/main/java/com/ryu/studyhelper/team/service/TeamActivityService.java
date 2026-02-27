package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.solve.service.SolveService;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.dto.internal.QueryPeriod;
import com.ryu.studyhelper.team.dto.response.TeamActivityResponseV2;
import com.ryu.studyhelper.team.dto.response.TeamLeaderboardResponse;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 팀 활동 현황 조회 서비스
 * 팀원들의 문제 풀이 현황과 리더보드를 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class TeamActivityService {

    private final TeamService teamService;
    private final SolveService solveService;
    private final TeamMemberRepository teamMemberRepository;
    private final SquadRepository squadRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;

    private static final int MAX_DAYS = 30;
    private static final int DEFAULT_DAYS = 30;

    // ========== 기간 계산 ==========

    private QueryPeriod calculateQueryPeriod(Integer days) {
        int queryDays = (days == null || days <= 0) ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);
        LocalDate endDate = MissionCyclePolicy.toMissionDate(LocalDateTime.now());
        LocalDate startDate = endDate.minusDays(queryDays - 1);
        return QueryPeriod.of(queryDays, startDate, endDate);
    }

    // ========== V2: 팀 리더보드 ==========
    @Transactional(readOnly = true)
    public TeamLeaderboardResponse getTeamLeaderboard(Long teamId, Long currentMemberId, Integer days) {
        teamService.validateTeamAccess(teamId, currentMemberId);

        QueryPeriod period = calculateQueryPeriod(days);
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdWithMember(teamId);
        List<Squad> squads = squadRepository.findByTeamIdOrderByIdAsc(teamId);
        Map<Long, Squad> squadMap = squads.stream().collect(Collectors.toMap(Squad::getId, s -> s));

        if (teamMembers.isEmpty()) {
            return new TeamLeaderboardResponse(currentMemberId,
                    new TeamLeaderboardResponse.Period(period.days(), period.startDate(), period.endDate()),
                    List.of());
        }

        List<MemberRecommendation> memberRecs = memberRecommendationRepository
                .findByTeamIdAndCreatedAtBetween(teamId, period.startDateTime(), period.endDateTime());

        Map<Long, Set<Long>> solvedMap = Map.of();
        if (!memberRecs.isEmpty()) {
            Set<Long> allProblemIds = memberRecs.stream()
                    .flatMap(mr -> mr.getRecommendation().getProblems().stream())
                    .map(rp -> rp.getProblem().getId())
                    .collect(Collectors.toSet());
            List<Long> participatingMemberIds = memberRecs.stream()
                    .map(mr -> mr.getMember().getId()).distinct().toList();
            solvedMap = solveService.getSolvedProblemIdMap(participatingMemberIds, allProblemIds);
        }

        return new TeamLeaderboardResponse(currentMemberId,
                new TeamLeaderboardResponse.Period(period.days(), period.startDate(), period.endDate()),
                buildLeaderboardRanks(teamMembers, memberRecs, solvedMap, squadMap));
    }

    private List<TeamLeaderboardResponse.MemberRank> buildLeaderboardRanks(
            List<TeamMember> teamMembers,
            List<MemberRecommendation> memberRecs,
            Map<Long, Set<Long>> solvedMap,
            Map<Long, Squad> squadMap) {

        Map<Long, Set<Long>> memberProblemIds = memberRecs.stream().collect(Collectors.groupingBy(
                mr -> mr.getMember().getId(),
                Collectors.flatMapping(
                        mr -> mr.getRecommendation().getProblems().stream().map(rp -> rp.getProblem().getId()),
                        Collectors.toSet()
                )
        ));

        record MemberScore(TeamMember tm, long solved) {}
        List<MemberScore> scores = teamMembers.stream()
                .map(tm -> {
                    Set<Long> pIds = memberProblemIds.getOrDefault(tm.getMember().getId(), Set.of());
                    Set<Long> solved = solvedMap.getOrDefault(tm.getMember().getId(), Set.of());
                    long count = pIds.stream().filter(solved::contains).count();
                    return new MemberScore(tm, count);
                })
                .sorted(Comparator.comparingLong(MemberScore::solved).reversed()
                        .thenComparingLong(s -> s.tm().getMember().getId()))
                .toList();

        List<TeamLeaderboardResponse.MemberRank> ranks = new ArrayList<>();
        int currentRank = 1;
        long previousSolved = -1;
        int sameRankCount = 0;

        for (MemberScore score : scores) {
            if (score.solved() != previousSolved) {
                currentRank += sameRankCount;
                sameRankCount = 1;
            } else {
                sameRankCount++;
            }
            previousSolved = score.solved();

            TeamMember tm = score.tm();
            Long squadId = tm.getSquadId();
            Squad squad = squadId != null ? squadMap.get(squadId) : null;
            ranks.add(new TeamLeaderboardResponse.MemberRank(
                    tm.getMember().getId(), tm.getMember().getHandle(),
                    squadId, squad != null ? squad.getName() : null,
                    currentRank, score.solved()
            ));
        }

        return ranks;
    }


    // ========== V2: 팀원별 활동 현황 ==========

    @Transactional(readOnly = true)
    public TeamActivityResponseV2 getTeamActivityV2(Long teamId, Long currentMemberId, Integer days) {
        teamService.validateTeamAccess(teamId, currentMemberId);

        QueryPeriod period = calculateQueryPeriod(days);
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdWithMember(teamId);

        if (teamMembers.isEmpty()) {
            return buildEmptyResponseV2(currentMemberId, period);
        }

        List<Squad> squads = squadRepository.findByTeamIdOrderByIdAsc(teamId);
        Map<Long, Squad> squadMap = squads.stream().collect(Collectors.toMap(Squad::getId, s -> s));

        List<MemberRecommendation> memberRecs = memberRecommendationRepository
                .findByTeamIdAndCreatedAtBetween(teamId, period.startDateTime(), period.endDateTime());

        Map<Long, Set<Long>> solvedMap = Map.of();
        if (!memberRecs.isEmpty()) {
            Set<Long> allProblemIds = memberRecs.stream()
                    .flatMap(mr -> mr.getRecommendation().getProblems().stream())
                    .map(rp -> rp.getProblem().getId())
                    .collect(Collectors.toSet());
            List<Long> participatingMemberIds = memberRecs.stream()
                    .map(mr -> mr.getMember().getId()).distinct().toList();
            solvedMap = solveService.getSolvedProblemIdMap(participatingMemberIds, allProblemIds);
        }

        Map<Long, List<MemberRecommendation>> recsByMember = memberRecs.stream()
                .collect(Collectors.groupingBy(mr -> mr.getMember().getId()));

        return new TeamActivityResponseV2(currentMemberId,
                new TeamActivityResponseV2.Period(period.days(), period.startDate(), period.endDate()),
                buildMemberActivities(teamMembers, recsByMember, solvedMap, squadMap)
        );
    }

    private List<TeamActivityResponseV2.MemberActivity> buildMemberActivities(
            List<TeamMember> teamMembers,
            Map<Long, List<MemberRecommendation>> recsByMember,
            Map<Long, Set<Long>> solvedMap,
            Map<Long, Squad> squadMap) {

        return teamMembers.stream()
                .map(tm -> {
                    Long memberId = tm.getMember().getId();
                    Long squadId = tm.getSquadId();
                    Squad squad = squadId != null ? squadMap.get(squadId) : null;
                    Set<Long> solved = solvedMap.getOrDefault(memberId, Set.of());

                    // 미션 날짜 기준으로 그룹핑 (같은 날 여러 MR 머지)
                    Map<LocalDate, List<MemberRecommendation>> byDate =
                            recsByMember.getOrDefault(memberId, List.of()).stream()
                                    .collect(Collectors.groupingBy(
                                            mr -> MissionCyclePolicy.toMissionDate(mr.getRecommendation().getCreatedAt())
                                    ));

                    List<TeamActivityResponseV2.DailyRecommendation> dailyRecs =
                            byDate.entrySet().stream()
                                    .sorted(Map.Entry.<LocalDate, List<MemberRecommendation>>comparingByKey().reversed())
                                    .map(entry -> {
                                        LocalDate date = entry.getKey();
                                        // 같은 날 MR들의 문제를 flat하게 수집, problemId 기준 중복 제거 후 정렬
                                        List<TeamActivityResponseV2.ProblemActivity> problems =
                                                entry.getValue().stream()
                                                        .flatMap(mr -> mr.getRecommendation().getProblems().stream())
                                                        .collect(Collectors.toMap(
                                                                rp -> rp.getProblem().getId(),
                                                                rp -> rp,
                                                                (a, b) -> a
                                                        ))
                                                        .values().stream()
                                                        .sorted(Comparator.comparing(rp -> rp.getProblem().getId()))
                                                        .map(rp -> new TeamActivityResponseV2.ProblemActivity(
                                                                rp.getProblem().getId(),
                                                                rp.getProblem().getTitle(),
                                                                rp.getProblem().getTitleKo(),
                                                                rp.getProblem().getLevel(),
                                                                solved.contains(rp.getProblem().getId())
                                                        ))
                                                        .toList();
                                        return new TeamActivityResponseV2.DailyRecommendation(date, problems);
                                    })
                                    .toList();

                    return new TeamActivityResponseV2.MemberActivity(
                            memberId,
                            tm.getMember().getHandle(),
                            squadId,
                            squad != null ? squad.getName() : null,
                            dailyRecs
                    );
                })
                .toList();
    }

    // ========== V2: 빈 응답 ==========

    private TeamActivityResponseV2 buildEmptyResponseV2(Long currentMemberId, QueryPeriod period) {
        return new TeamActivityResponseV2(currentMemberId,
                new TeamActivityResponseV2.Period(period.days(), period.startDate(), period.endDate()),
                List.of());
    }
}
