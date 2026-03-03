package com.ryu.studyhelper.solve.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.repository.ProblemRepository;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.solve.domain.MemberSolvedProblem;
import com.ryu.studyhelper.solve.dto.response.DailySolvedResponse;
import com.ryu.studyhelper.solve.repository.MemberSolvedProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SolveService {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final MemberRecommendationRepository memberRecommendationRepository;
    private final MemberSolvedProblemRepository memberSolvedProblemRepository;
    private final Clock clock;

    private static final int MAX_DAILY_SOLVED_DAYS = 730;

    @Transactional(readOnly = true)
    public String validateAndGetHandle(Long memberId, Long problemId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        if (member.getHandle() == null || member.getHandle().isEmpty()) {
            throw new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        }
        problemRepository.findById(problemId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.PROBLEM_NOT_FOUND));
        if (!memberRecommendationRepository.existsByMemberIdAndRecommendedProblemId(memberId, problemId)) {
            throw new CustomException(CustomResponseStatus.PROBLEM_NOT_IN_RECOMMENDATION);
        }
        if (memberSolvedProblemRepository.existsByMemberIdAndProblemId(memberId, problemId)) {
            throw new CustomException(CustomResponseStatus.ALREADY_SOLVED);
        }

        return member.getHandle();
    }

    @Transactional
    public void recordSolved(Long memberId, Long problemId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.PROBLEM_NOT_FOUND));
        memberSolvedProblemRepository.save(MemberSolvedProblem.create(member, problem));
    }

    @Transactional(readOnly = true)
    public DailySolvedResponse getDailySolved(Long memberId, int days) {
        if (days < 1 || days > MAX_DAILY_SOLVED_DAYS) {
            throw new CustomException(CustomResponseStatus.INVALID_DAYS_RANGE);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = getAdjustedDate(now);

        LocalDateTime startDateTime = today.minusDays(days - 1).atTime(LocalTime.of(6, 0));
        LocalDateTime endDateTime = today.plusDays(1).atTime(LocalTime.of(6, 0));

        List<MemberSolvedProblem> solvedProblems = memberSolvedProblemRepository
                .findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(memberId, startDateTime, endDateTime);

        Map<LocalDate, List<DailySolvedResponse.SolvedProblem>> groupedByDate = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            groupedByDate.put(today.minusDays(i), new ArrayList<>());
        }

        for (MemberSolvedProblem solved : solvedProblems) {
            LocalDate adjustedDate = getAdjustedDate(solved.getSolvedAt());
            Problem problem = solved.getProblem();

            DailySolvedResponse.SolvedProblem solvedProblem = new DailySolvedResponse.SolvedProblem(
                    problem.getId(),
                    problem.getTitleKo() != null ? problem.getTitleKo() : problem.getTitle(),
                    problem.getLevel()
            );

            if (groupedByDate.containsKey(adjustedDate)) {
                groupedByDate.get(adjustedDate).add(solvedProblem);
            }
        }

        List<DailySolvedResponse.DailySolved> dailySolvedList = groupedByDate.entrySet().stream()
                .map(entry -> new DailySolvedResponse.DailySolved(
                        entry.getKey().toString(),
                        entry.getValue().size(),
                        entry.getValue()
                ))
                .toList();

        int totalCount = dailySolvedList.stream()
                .mapToInt(DailySolvedResponse.DailySolved::count)
                .sum();

        return new DailySolvedResponse(dailySolvedList, totalCount);
    }

    @Transactional(readOnly = true)
    public long countByMemberId(Long memberId) {
        return memberSolvedProblemRepository.countByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Set<Long>> getSolvedProblemIdMap(List<Long> memberIds, Collection<Long> problemIds) {
        if (memberIds.isEmpty() || problemIds.isEmpty()) {
            return Map.of();
        }
        return memberSolvedProblemRepository
                .findByMemberIdsAndProblemIds(memberIds, new ArrayList<>(problemIds))
                .stream()
                .collect(Collectors.groupingBy(
                        msp -> msp.getMember().getId(),
                        Collectors.mapping(msp -> msp.getProblem().getId(), Collectors.toSet())
                ));
    }

    private LocalDate getAdjustedDate(LocalDateTime dateTime) {
        if (dateTime.getHour() < 6) {
            return dateTime.toLocalDate().minusDays(1);
        }
        return dateTime.toLocalDate();
    }
}
