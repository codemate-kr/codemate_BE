package com.ryu.studyhelper.solve.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.repository.ProblemRepository;
import com.ryu.studyhelper.solve.domain.MemberSolvedProblem;
import org.springframework.dao.DataIntegrityViolationException;
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
@Transactional
public class SolveService {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final MemberSolvedProblemRepository memberSolvedProblemRepository;
    private final SolvedAcClient solvedAcClient;
    private final Clock clock;

    private static final int MAX_DAILY_SOLVED_DAYS = 730;

    /**
     * 문제 해결 인증
     * @param memberId 회원 ID
     * @param problemId BOJ 문제 번호
     */
    public void verifyProblemSolved(Long memberId, Long problemId) {
        // 1. Member 조회
        Member member = findMemberById(memberId);

        // 2. 핸들 존재 여부 확인 (조기 검증)
        if (member.getHandle() == null || member.getHandle().isEmpty()) {
            throw new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);
        }

        // 3. Problem 조회 (DB에 존재하는지 확인)
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.PROBLEM_NOT_FOUND));

        // 4. 이미 인증된 문제인지 확인
        if (memberSolvedProblemRepository.existsByMemberIdAndProblemId(memberId, problemId)) {
            throw new CustomException(CustomResponseStatus.ALREADY_SOLVED);
        }

        // 5. solved.ac API로 실제 해결 여부 검증
        boolean isSolved = solvedAcClient.hasUserSolvedProblem(member.getHandle(), problemId);

        if (!isSolved) {
            throw new CustomException(CustomResponseStatus.PROBLEM_NOT_SOLVED_YET);
        }

        // 6. MemberSolvedProblem 레코드 생성
        MemberSolvedProblem memberSolvedProblem = MemberSolvedProblem.create(member, problem);
        try {
            memberSolvedProblemRepository.save(memberSolvedProblem);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(CustomResponseStatus.ALREADY_SOLVED);
        }
    }

    /**
     * 최근 N일간 일별 문제 풀이 현황 조회
     * - 날짜 기준: 오전 6시 (06:00 ~ 다음날 05:59를 하루로 계산)
     * @param memberId 회원 ID
     * @param days 조회할 일수 (1~730일)
     * @return 일별 풀이 현황
     */
    @Transactional(readOnly = true)
    public DailySolvedResponse getDailySolved(Long memberId, int days) {
        if (days < 1 || days > MAX_DAILY_SOLVED_DAYS) {
            throw new CustomException(CustomResponseStatus.INVALID_DAYS_RANGE);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = getAdjustedDate(now);

        // 조회 범위: (days-1)일 전 06:00 <= solvedAt < 내일 06:00
        LocalDateTime startDateTime = today.minusDays(days - 1).atTime(LocalTime.of(6, 0));
        LocalDateTime endDateTime = today.plusDays(1).atTime(LocalTime.of(6, 0));

        List<MemberSolvedProblem> solvedProblems = memberSolvedProblemRepository
                .findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(memberId, startDateTime, endDateTime);

        // 날짜별로 그룹핑 (오전 6시 기준)
        Map<LocalDate, List<DailySolvedResponse.SolvedProblem>> groupedByDate = new LinkedHashMap<>();

        // 먼저 모든 날짜를 빈 리스트로 초기화 (과거 → 현재 순서)
        for (int i = days - 1; i >= 0; i--) {
            groupedByDate.put(today.minusDays(i), new ArrayList<>());
        }

        // 풀이 데이터를 날짜별로 분류
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

        // 응답 생성
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

    /**
     * 회원의 총 풀이 수 조회
     * @param memberId 회원 ID
     * @return 풀이 수
     */
    @Transactional(readOnly = true)
    public long countByMemberId(Long memberId) {
        return memberSolvedProblemRepository.countByMemberId(memberId);
    }

    /**
     * 오전 6시 기준으로 날짜 계산
     * - 06:00 이전이면 전날로 처리
     */
    private LocalDate getAdjustedDate(LocalDateTime dateTime) {
        if (dateTime.getHour() < 6) {
            return dateTime.toLocalDate().minusDays(1);
        }
        return dateTime.toLocalDate();
    }

    /**
     * 여러 멤버의 특정 문제 풀이 여부 맵 조회
     * @param memberIds 멤버 ID 목록
     * @param problemIds 문제 ID 컬렉션
     * @return memberId → 풀이한 problemId 집합
     */
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

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));
    }
}
