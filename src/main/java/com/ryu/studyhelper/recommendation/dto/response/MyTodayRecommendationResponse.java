package com.ryu.studyhelper.recommendation.dto.response;

import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendationProblem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 개인 대시보드 - 오늘의 모든 추천 문제 조회 응답 DTO
 * 유저가 속한 모든 팀의 오늘 추천 문제들을 평탄화된 리스트로 표시합니다.
 * 각 문제별로 어느 팀에서 추천받았는지 팀명이 함께 표시됩니다.
 */
public record MyTodayRecommendationResponse(
        List<RecommendedProblemWithTeam> problems
) {
    /**
     * 추천 문제 상세 정보 (팀명 및 개인별 해결 여부 포함)
     */
    public record RecommendedProblemWithTeam(
            Long memberRecommendationProblemId,
            Long problemId,
            String title,
            String titleKo,
            Integer level,
            String url,
            Integer acceptedUserCount,
            Double averageTries,
            Long teamId,
            String teamName,
            boolean isSolved,
            LocalDateTime solvedAt,
            LocalDateTime recommendedAt
    ) {}

    /**
     * MemberRecommendationProblem 리스트로부터 응답 DTO 생성
     * 모든 문제를 평탄화된 리스트로 반환하며, 각 문제는 팀 정보를 포함합니다.
     *
     * @param problems 개인 추천 문제 목록
     * @return 평탄화된 추천 문제 리스트 (팀명 포함)
     */
    public static MyTodayRecommendationResponse from(List<MemberRecommendationProblem> problems) {
        List<RecommendedProblemWithTeam> problemList = problems.stream()
                .map(mrp -> new RecommendedProblemWithTeam(
                        mrp.getId(),
                        mrp.getProblem().getId(),
                        mrp.getProblem().getTitle(),
                        mrp.getProblem().getTitleKo(),
                        mrp.getProblem().getLevel(),
                        mrp.getProblem().getUrl(),
                        mrp.getProblem().getAcceptedUserCount(),
                        mrp.getProblem().getAverageTries(),
                        mrp.getTeamId(),
                        mrp.getTeamName(),
                        mrp.isSolved(),
                        mrp.getSolvedAt(),
                        mrp.getCreatedAt()
                ))
                .toList();

        return new MyTodayRecommendationResponse(problemList);
    }
}