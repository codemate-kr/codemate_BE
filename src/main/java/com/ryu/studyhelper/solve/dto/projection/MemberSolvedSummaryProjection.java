package com.ryu.studyhelper.solve.dto.projection;

/**
 * 멤버별 기간 내 풀이 수 집계 Projection
 * 리더보드 생성에 사용됩니다.
 */
public interface MemberSolvedSummaryProjection {
    Long getMemberId();
    String getHandle();
    Long getTotalSolved();
}
