package com.ryu.studyhelper.member;

import com.ryu.studyhelper.member.domain.MemberSolvedProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberSolvedProblemRepository extends JpaRepository<MemberSolvedProblem, Long> {

    boolean existsByMemberIdAndProblemId(Long memberId, Long problemId);

    List<MemberSolvedProblem> findByMemberId(Long memberId);

    List<MemberSolvedProblem> findByMemberIdAndProblemIdIn(Long memberId, List<Long> problemIds);
}