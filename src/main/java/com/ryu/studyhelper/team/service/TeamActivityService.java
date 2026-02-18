package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.solve.domain.MemberSolvedProblem;
import com.ryu.studyhelper.solve.repository.MemberSolvedProblemRepository;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.dto.internal.MemberSolvedStatus;
import com.ryu.studyhelper.team.dto.internal.QueryPeriod;
import com.ryu.studyhelper.solve.dto.projection.MemberSolvedSummaryProjection;
import com.ryu.studyhelper.team.dto.response.TeamActivityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class TeamActivityService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RecommendationRepository recommendationRepository;
    private final MemberSolvedProblemRepository memberSolvedProblemRepository;

    private static final int MAX_DAYS = 30;
    private static final int DEFAULT_DAYS = 30;

    /**
     * 팀 활동 현황 조회
     * TODO: 추후 readOnly 고려
     */
    @Transactional
    public TeamActivityResponse getTeamActivity(Long teamId, Long currentMemberId, Integer days) {
        Team team = findTeamOrThrow(teamId);
        validatePrivateTeamAccess(team, currentMemberId);

        QueryPeriod period = calculateQueryPeriod(days);
        List<Member> members = teamMemberRepository.findMembersByTeamId(teamId);

        if (members.isEmpty()) {
            return buildEmptyResponse(currentMemberId, period);
        }

        // 기간 내 추천 조회 (리더보드와 일별 활동에서 공통 사용)
        List<Recommendation> recommendations =
                recommendationRepository.findByTeamIdAndCreatedAtBetweenWithProblems(
                        teamId, period.startDateTime(), period.endDateTime());

        Set<Long> recommendedProblemIds = extractAllProblemIds(recommendations);

        List<TeamActivityResponse.MemberRank> memberRanks = buildMemberRanks(members, recommendedProblemIds);
        List<TeamActivityResponse.DailyActivity> dailyActivities = buildDailyActivities(recommendations, members, recommendedProblemIds);

        return TeamActivityResponse.of(
                currentMemberId,
                new TeamActivityResponse.Period(period.days(), period.startDate(), period.endDate()),
                memberRanks,
                dailyActivities
        );
    }

    // ========== 팀 조회 ==========

    private Team findTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));
    }

    // ========== 권한 검증 ==========

    private void validatePrivateTeamAccess(Team team, Long memberId) {
        if (!team.getIsPrivate()) {
            return;
        }
        if (memberId == null) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
        if (!teamMemberRepository.existsByTeamIdAndMemberId(team.getId(), memberId)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }

    // ========== 기간 계산 ==========

    private QueryPeriod calculateQueryPeriod(Integer days) {
        int queryDays = calculateDays(days);
        LocalDate endDate = MissionCyclePolicy.toMissionDate(LocalDateTime.now());
        LocalDate startDate = endDate.minusDays(queryDays - 1);
        return QueryPeriod.of(queryDays, startDate, endDate);
    }

    private int calculateDays(Integer days) {
        if (days == null || days <= 0) {
            return DEFAULT_DAYS;
        }
        return Math.min(days, MAX_DAYS);
    }

    // ========== 리더보드 구성 ==========

    /**
     * 리더보드 구성 (팀 추천 문제 기준)
     */
    private List<TeamActivityResponse.MemberRank> buildMemberRanks(List<Member> members, Set<Long> recommendedProblemIds) {
        if (recommendedProblemIds.isEmpty()) {
            // 추천 문제가 없으면 모든 멤버 0문제로 처리 (동률 1위, 핸들순 정렬)
            return members.stream()
                    .sorted(Comparator.comparing(Member::getHandle, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(m -> new TeamActivityResponse.MemberRank(m.getId(), m.getHandle(), 1, 0))
                    .toList();
        }

        List<Long> memberIds = members.stream().map(Member::getId).toList();
        List<MemberSolvedSummaryProjection> summaries =
                memberSolvedProblemRepository.countSolvedByMemberIdsAndProblemIds(
                        memberIds, new ArrayList<>(recommendedProblemIds));

        return assignRanks(summaries);
    }

    /**
     * 순위 부여 (동점자 처리: 공동 2위 2명이면 다음은 4위)
     */
    private List<TeamActivityResponse.MemberRank> assignRanks(List<MemberSolvedSummaryProjection> summaries) {
        List<TeamActivityResponse.MemberRank> ranks = new ArrayList<>();
        int currentRank = 1;
        long previousSolved = -1;
        int sameRankCount = 0;

        for (MemberSolvedSummaryProjection summary : summaries) {
            long totalSolved = summary.getTotalSolved();

            if (totalSolved != previousSolved) {
                currentRank += sameRankCount;
                sameRankCount = 1;
            } else {
                sameRankCount++;
            }

            ranks.add(new TeamActivityResponse.MemberRank(
                    summary.getMemberId(),
                    summary.getHandle(),
                    currentRank,
                    (int) totalSolved
            ));

            previousSolved = totalSolved;
        }

        return ranks;
    }

    // ========== 일별 활동 구성 ==========

    private List<TeamActivityResponse.DailyActivity> buildDailyActivities(
            List<Recommendation> recommendations,
            List<Member> members,
            Set<Long> recommendedProblemIds) {

        if (recommendations.isEmpty() || recommendedProblemIds.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = members.stream().map(Member::getId).toList();
        List<MemberSolvedStatus> memberSolvedStatuses = loadMemberSolvedStatuses(memberIds, recommendedProblemIds);

        return recommendations.stream()
                .map(rec -> buildDailyActivity(rec, memberSolvedStatuses))
                .toList();
    }

    private Set<Long> extractAllProblemIds(List<Recommendation> recommendations) {
        return recommendations.stream()
                .flatMap(r -> r.getProblems().stream())
                .map(rp -> rp.getProblem().getId())
                .collect(Collectors.toSet());
    }

    private List<MemberSolvedStatus> loadMemberSolvedStatuses(List<Long> memberIds, Set<Long> problemIds) {
        List<MemberSolvedProblem> solvedRecords =
                memberSolvedProblemRepository.findByMemberIdsAndProblemIds(memberIds, new ArrayList<>(problemIds));

        Map<Long, Set<Long>> memberSolvedMap = solvedRecords.stream()
                .collect(Collectors.groupingBy(
                        msp -> msp.getMember().getId(),
                        Collectors.mapping(msp -> msp.getProblem().getId(), Collectors.toSet())
                ));

        return memberIds.stream()
                .map(memberId -> new MemberSolvedStatus(
                        memberId,
                        memberSolvedMap.getOrDefault(memberId, Set.of())
                ))
                .toList();
    }

    private TeamActivityResponse.DailyActivity buildDailyActivity(
            Recommendation recommendation,
            List<MemberSolvedStatus> memberSolvedStatuses) {

        // 미션 사이클 기준 날짜 (6시 이전 생성분은 전날로 표시)
        LocalDate date = MissionCyclePolicy.toMissionDate(recommendation.getCreatedAt());

        List<TeamActivityResponse.ProblemInfo> problems = buildProblemInfoList(recommendation);
        List<Long> problemIds = problems.stream()
                .map(TeamActivityResponse.ProblemInfo::problemId)
                .toList();

        List<TeamActivityResponse.MemberSolved> memberSolvedList =
                buildMemberSolvedList(memberSolvedStatuses, problemIds);

        return new TeamActivityResponse.DailyActivity(date, problems, memberSolvedList);
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
            List<MemberSolvedStatus> memberSolvedStatuses,
            List<Long> problemIds) {

        return memberSolvedStatuses.stream()
                .map(status -> {
                    Map<Long, Boolean> solvedMap = problemIds.stream()
                            .collect(Collectors.toMap(
                                    pid -> pid,
                                    status::hasSolved
                            ));
                    return new TeamActivityResponse.MemberSolved(status.memberId(), solvedMap);
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
}