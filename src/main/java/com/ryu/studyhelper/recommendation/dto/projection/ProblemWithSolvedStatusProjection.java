package com.ryu.studyhelper.recommendation.dto.projection;

/**
 * 문제와 해결 여부를 함께 조회하는 Projection
 * OUTER JOIN 쿼리 결과를 매핑합니다.
 */
public interface ProblemWithSolvedStatusProjection {
    Long getProblemId();
    String getTitle();
    String getTitleKo();
    Integer getLevel();
    Integer getAcceptedUserCount();
    Double getAverageTries();
    Boolean getIsSolved();
}