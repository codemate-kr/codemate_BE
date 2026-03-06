package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.service.ProblemSyncService;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.repository.SquadIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 스쿼드 추천 생성 공통 로직
 * - createForSquad: 수동 추천 (핸들 체크 포함, FAILED 레코드 재사용)
 * - process: 배치/재시도 배치 (기존 PENDING 레코드 처리)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationCreator {

    /**
     * 수동 추천 생성 결과 (REQUIRES_NEW 커밋 후 DB 재조회 없이 문제/멤버 목록 전달)
     */
    public record CreationResult(Recommendation recommendation, List<Problem> problems, List<MemberRecommendation> memberRecommendations) {}

    private final TeamMemberRepository teamMemberRepository;
    private final SquadIncludeTagRepository squadIncludeTagRepository;
    private final SolvedAcClient solvedAcClient;
    private final ProblemSyncService problemSyncService;
    private final RecommendationSaver recommendationSaver;

    /**
     * 수동 추천 생성.
     * 인증된 핸들이 없으면 {@link Optional#empty()} 반환.
     * API 실패 시 FAILED로 저장 후 예외 전파.
     *
     * saveSuccess는 REQUIRES_NEW로 독립 커밋되므로, 호출자 트랜잭션의 REPEATABLE_READ 스냅샷에서
     * 새로 커밋된 문제 행이 보이지 않는 문제를 방지하기 위해 문제 목록을 함께 반환한다.
     */
    public Optional<CreationResult> createForSquad(Squad squad, RecommendationType type, LocalDate date) {
        Long teamId = squad.getTeam().getId();
        List<String> handles = teamMemberRepository.findHandlesByTeamIdAndSquadId(teamId, squad.getId());
        if (handles.isEmpty()) {
            log.info("스쿼드 '{}'에 인증된 핸들이 없어 추천을 스킵합니다", squad.getName());
            return Optional.empty();
        }

        Recommendation pending = recommendationSaver.createOrResetPending(squad, date, type);
        CreationResult result = processInternal(pending, squad, handles);
        return Optional.of(result);
    }

    /**
     * 배치/재시도 배치용 처리.
     * 기존 PENDING 레코드를 받아 API 호출 → SUCCESS or FAILED 업데이트.
     * 실패 시 FAILED로 저장 후 예외 전파.
     */
    public void process(Recommendation pending, Squad squad) {
        Long teamId = squad.getTeam().getId();
        List<String> handles = teamMemberRepository.findHandlesByTeamIdAndSquadId(teamId, squad.getId());
        if (handles.isEmpty()) {
            log.warn("스쿼드 '{}'의 인증된 핸들이 없어 FAILED 처리합니다", squad.getName());
            recommendationSaver.saveFailed(pending);
            throw new IllegalStateException("인증된 핸들 없음: " + squad.getName());
        }
        processInternal(pending, squad, handles);
    }

    private CreationResult processInternal(Recommendation pending, Squad squad, List<String> handles) {
        try {
            List<Problem> problems = recommendProblemsForSquad(squad, handles);
            Long teamId = squad.getTeam().getId();
            List<Member> members = teamMemberRepository.findMembersByTeamIdAndSquadId(teamId, squad.getId());
            List<MemberRecommendation> memberRecommendations = recommendationSaver.saveSuccess(pending, problems, members, squad);
            log.info("추천 생성 완료 - 팀: {}, 스쿼드: {}, 문제: {}개",
                    squad.getTeam().getName(), squad.getName(), problems.size());
            return new CreationResult(pending, problems, memberRecommendations);
        } catch (Exception e) {
            recommendationSaver.saveFailed(pending);
            throw e;
        }
    }

    private List<Problem> recommendProblemsForSquad(Squad squad, List<String> handles) {
        List<String> tagKeys = squadIncludeTagRepository.findTagKeysBySquadId(squad.getId());

        List<ProblemInfo> problemInfos = solvedAcClient.recommendUnsolvedProblems(
                handles,
                squad.getProblemCount(),
                squad.getEffectiveMinProblemLevel(),
                squad.getEffectiveMaxProblemLevel(),
                tagKeys
        );

        return problemSyncService.syncProblems(problemInfos);
    }
}
