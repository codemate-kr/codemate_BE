package com.ryu.studyhelper.recommendation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 로그인한 유저의 모든 팀 오늘의 문제 조회 응답 DTO
 */
public record MyTodayProblemsResponse(
        List<TeamTodayProblems> teams,
        int totalProblemCount,
        int solvedCount
) {
    /**
     * 팀별 오늘의 문제
     */
    public record TeamTodayProblems(
            Long teamId,
            String teamName,
            Long recommendationId,
            LocalDateTime createdAt,
            List<TodayProblemResponse.ProblemWithSolvedStatus> problems
    ) {
        public static TeamTodayProblems from(
                Long teamId,
                String teamName,
                TodayProblemResponse todayProblemResponse
        ) {
            return new TeamTodayProblems(
                    teamId,
                    teamName,
                    todayProblemResponse.recommendationId(),
                    todayProblemResponse.createdAt(),
                    todayProblemResponse.problems()
            );
        }
    }

    public static MyTodayProblemsResponse from(List<TeamTodayProblems> teams) {
        int totalProblemCount = teams.stream()
                .mapToInt(team -> team.problems().size())
                .sum();

        int solvedCount = teams.stream()
                .flatMap(team -> team.problems().stream())
                .filter(problem -> Boolean.TRUE.equals(problem.isSolved()))
                .mapToInt(problem -> 1)
                .sum();

        return new MyTodayProblemsResponse(teams, totalProblemCount, solvedCount);
    }
}