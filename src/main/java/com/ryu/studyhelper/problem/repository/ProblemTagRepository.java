package com.ryu.studyhelper.problem.repository;

import com.ryu.studyhelper.problem.domain.ProblemTag;
import com.ryu.studyhelper.problem.dto.projection.ProblemTagProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemTagRepository extends JpaRepository<ProblemTag, Long> {

    List<ProblemTag> findByProblemId(Long problemId);

    List<ProblemTag> findByTagKey(String tagKey);

    @Modifying
    @Query("DELETE FROM ProblemTag pt WHERE pt.problem.id = :problemId")
    void deleteAllByProblemId(@Param("problemId") Long problemId);

    boolean existsByProblemIdAndTagKey(Long problemId, String tagKey);

    @Query("SELECT pt FROM ProblemTag pt JOIN FETCH pt.tag WHERE pt.problem.id = :problemId")
    List<ProblemTag> findByProblemIdWithTag(@Param("problemId") Long problemId);

    @Query("SELECT pt FROM ProblemTag pt JOIN FETCH pt.problem WHERE pt.tag.key = :tagKey")
    List<ProblemTag> findByTagKeyWithProblem(@Param("tagKey") String tagKey);

    /**
     * 문제 ID 목록으로 태그 정보 조회
     * @param problemIds 문제 ID 목록
     * @return 문제별 태그 정보 (problemId, tagKey, nameKo, nameEn)
     */
    @Query("""
            SELECT pt.problem.id AS problemId,
                   t.key AS tagKey,
                   t.nameKo AS nameKo,
                   t.nameEn AS nameEn
            FROM ProblemTag pt
            JOIN pt.tag t
            WHERE pt.problem.id IN :problemIds
            """)
    List<ProblemTagProjection> findTagsByProblemIds(@Param("problemIds") List<Long> problemIds);
}