package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.solve.service.SolveService;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.dto.internal.QueryPeriod;
import com.ryu.studyhelper.team.dto.response.TeamActivityResponse;
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
    private final RecommendationRepository recommendationRepository;
    private final SquadRepository squadRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;

    private static final int MAX_DAYS = 30;
    private static final int DEFAULT_DAYS = 30;

    // TODO: V1 API 제거 예정 - getTeamActivity, buildMemberRanks, buildDailyActivities,
    //       buildProblemInfoList, buildMemberSolvedList, buildEmptyResponse 및
    //       관련 import (TeamActivityResponse, RecommendationRepository, Recommendation,
    //       RecommendationProblem) 일괄 삭제
    @Transactional
    public TeamActivityResponse getTeamActivity(Long teamId, Long currentMemberId, Integer days) {
        teamService.validateTeamAccess(teamId, currentMemberId);

        QueryPeriod period = calculateQueryPeriod(days);
        List<Member> members = teamMemberRepository.findMembersByTeamId(teamId);

        if (members.isEmpty()) {
            return buildEmptyResponse(currentMemberId, period);
        }

        List<Recommendation> recommendations =
                recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(
                        teamId, period.startDateTime(), period.endDateTime());

        Set<Long> recommendedProblemIds = recommendations.stream()
                .flatMap(r -> r.getProblems().stream())
                .map(rp -> rp.getProblem().getId())
                .collect(Collectors.toSet());

        List<Long> memberIds = members.stream().map(Member::getId).toList();
        Map<Long, Set<Long>> solvedMap = solveService.getSolvedProblemIdMap(memberIds, recommendedProblemIds);

        return TeamActivityResponse.of(
                currentMemberId,
                new TeamActivityResponse.Period(period.days(), period.startDate(), period.endDate()),
                buildMemberRanks(members, recommendedProblemIds, solvedMap),
                buildDailyActivities(recommendations, memberIds, solvedMap)
        );
    }

    // ========== 기간 계산 ==========

    private QueryPeriod calculateQueryPeriod(Integer days) {
        int queryDays = (days == null || days <= 0) ? DEFAULT_DAYS : Math.min(days, MAX_DAYS);
        LocalDate endDate = MissionCyclePolicy.toMissionDate(LocalDateTime.now());
        LocalDate startDate = endDate.minusDays(queryDays - 1);
        return QueryPeriod.of(queryDays, startDate, endDate);
    }

    // ========== 리더보드 구성 ==========

    private List<TeamActivityResponse.MemberRank> buildMemberRanks(
            List<Member> members,
            Set<Long> recommendedProblemIds,
            Map<Long, Set<Long>> solvedMap) {

        record MemberScore(Member member, long solved) {}
        List<MemberScore> scores = members.stream()
                .map(m -> {
                    Set<Long> solved = solvedMap.getOrDefault(m.getId(), Set.of());
                    long count = recommendedProblemIds.stream().filter(solved::contains).count();
                    return new MemberScore(m, count);
                })
                .sorted(Comparator.comparingLong(MemberScore::solved).reversed()
                        .thenComparing(s -> s.member().getHandle(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<TeamActivityResponse.MemberRank> ranks = new ArrayList<>();
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
            ranks.add(new TeamActivityResponse.MemberRank(
                    score.member().getId(), score.member().getHandle(),
                    currentRank, (int) score.solved()
            ));
            previousSolved = score.solved();
        }

        return ranks;
    }

    // ========== 일별 활동 구성 ==========

    private List<TeamActivityResponse.DailyActivity> buildDailyActivities(
            List<Recommendation> recommendations,
            List<Long> memberIds,
            Map<Long, Set<Long>> solvedMap) {

        if (recommendations.isEmpty()) {
            return List.of();
        }

        return recommendations.stream()
                .map(rec -> {
                    LocalDate date = MissionCyclePolicy.toMissionDate(rec.getCreatedAt());
                    List<TeamActivityResponse.ProblemInfo> problems = buildProblemInfoList(rec);
                    List<Long> problemIds = problems.stream().map(TeamActivityResponse.ProblemInfo::problemId).toList();
                    return new TeamActivityResponse.DailyActivity(date, problems, buildMemberSolvedList(memberIds, problemIds, solvedMap));
                })
                .toList();
    }

    private List<TeamActivityResponse.ProblemInfo> buildProblemInfoList(Recommendation recommendation) {
        return recommendation.getProblems().stream()
                .sorted(Comparator.comparing(RecommendationProblem::getId))
                .map(rp -> new TeamActivityResponse.ProblemInfo(
                        rp.getProblem().getId(),
                        rp.getProblem().getTitleKo() != null ?
                                rp.getProblem().getTitleKo() : rp.getProblem().getTitle(),
                        rp.getProblem().getLevel()
                ))
                .toList();
    }

    private List<TeamActivityResponse.MemberSolved> buildMemberSolvedList(
            List<Long> memberIds,
            List<Long> problemIds,
            Map<Long, Set<Long>> solvedMap) {

        return memberIds.stream()
                .map(memberId -> {
                    Set<Long> solved = solvedMap.getOrDefault(memberId, Set.of());
                    Map<Long, Boolean> solvedResult = problemIds.stream()
                            .collect(Collectors.toMap(pid -> pid, solved::contains));
                    return new TeamActivityResponse.MemberSolved(memberId, solvedResult);
                })
                .toList();
    }

    // ========== 빈 응답 ==========

    private TeamActivityResponse buildEmptyResponse(Long currentMemberId, QueryPeriod period) {
        return TeamActivityResponse.of(
                currentMemberId,
                new TeamActivityResponse.Period(period.days(), period.startDate(), period.endDate()),
                List.of(),
                List.of()
        );
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

                    List<TeamActivityResponseV2.DailyRecommendation> dailyRecs =
                            recsByMember.getOrDefault(memberId, List.of()).stream()
                                    .sorted(Comparator.comparing(
                                            mr -> MissionCyclePolicy.toMissionDate(mr.getRecommendation().getCreatedAt()),
                                            Comparator.reverseOrder()))
                                    .map(mr -> {
                                        LocalDate date = MissionCyclePolicy.toMissionDate(mr.getRecommendation().getCreatedAt());
                                        List<TeamActivityResponseV2.ProblemActivity> problems =
                                                mr.getRecommendation().getProblems().stream()
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
