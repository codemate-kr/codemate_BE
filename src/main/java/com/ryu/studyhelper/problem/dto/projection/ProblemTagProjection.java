package com.ryu.studyhelper.problem.dto.projection;

/**
 * 문제별 태그 정보 조회용 Projection
 */
public interface ProblemTagProjection {
    Long getProblemId();
    String getTagKey();
    String getNameKo();
    String getNameEn();
}