package com.ryu.studyhelper.member;

import com.ryu.studyhelper.member.domain.MemberSolvedProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberSolvedProblemRepository extends JpaRepository<MemberSolvedProblem, Long> {

}