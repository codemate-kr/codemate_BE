package com.ryu.studyhelper.solve.repository;

import com.ryu.studyhelper.solve.domain.MemberSolvedProblem;
import com.ryu.studyhelper.solve.dto.projection.GlobalRankingProjection;
import com.ryu.studyhelper.solve.dto.projection.MemberSolvedSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MemberSolvedProblemRepository extends JpaRepository<MemberSolvedProblem, Long> {

    boolean existsByMemberIdAndProblemId(Long memberId, Long problemId);

    List<MemberSolvedProblem> findByMemberId(Long memberId);

    List<MemberSolvedProblem> findByMemberIdAndProblemIdIn(Long memberId, List<Long> problemIds);

    long countByMemberId(Long memberId);

    List<MemberSolvedProblem> findByMemberIdAndSolvedAtGreaterThanEqualAndSolvedAtLessThanOrderBySolvedAtAsc(
            Long memberId, LocalDateTime start, LocalDateTime end);


    /**
     * 여러 멤버의 특정 문제들에 대한 풀이 여부 조회
     * @param memberIds 멤버 ID 목록
     * @param problemIds 문제 ID 목록
     * @return 멤버-문제 풀이 기록
     */
    @Query("SELECT msp FROM MemberSolvedProblem msp WHERE msp.member.id IN :memberIds AND msp.problem.id IN :problemIds")
    List<MemberSolvedProblem> findByMemberIdsAndProblemIds(
            @Param("memberIds") List<Long> memberIds,
            @Param("problemIds") List<Long> problemIds
    );

    /**
     * 멤버별 특정 문제들에 대한 풀이 수 집계 (팀 추천 문제 기준 리더보드용)
     * @param memberIds 멤버 ID 목록
     * @param problemIds 팀에서 추천한 문제 ID 목록
     * @return 멤버별 풀이 수 집계 (풀이 수 내림차순, 핸들 오름차순)
     */
    @Query("""
            SELECT m.id AS memberId, m.handle AS handle, COUNT(msp.id) AS totalSolved
            FROM Member m
            LEFT JOIN MemberSolvedProblem msp ON msp.member.id = m.id
                AND msp.problem.id IN :problemIds
            WHERE m.id IN :memberIds
            GROUP BY m.id, m.handle
            ORDER BY COUNT(msp.id) DESC, m.handle ASC
            """)
    List<MemberSolvedSummaryProjection> countSolvedByMemberIdsAndProblemIds(
            @Param("memberIds") List<Long> memberIds,
            @Param("problemIds") List<Long> problemIds
    );

    /**
     * 전체 랭킹 조회 (상위 N명)
     * @param limit 조회할 상위 인원 수
     * @return 핸들이 있는 멤버 중 풀이 수 기준 상위 N명 (풀이 수 내림차순, 핸들 오름차순)
     */
    @Query("""
            SELECT m.handle AS handle, COUNT(msp.id) AS totalSolved
            FROM Member m
            LEFT JOIN MemberSolvedProblem msp ON msp.member.id = m.id
            WHERE m.handle IS NOT NULL
            GROUP BY m.id, m.handle
            ORDER BY COUNT(msp.id) DESC, m.handle ASC
            LIMIT :limit
            """)
    List<GlobalRankingProjection> findGlobalRanking(@Param("limit") int limit);
}
