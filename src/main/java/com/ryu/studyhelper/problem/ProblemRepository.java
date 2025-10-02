package com.ryu.studyhelper.problem;

import com.ryu.studyhelper.problem.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // 문제 ID로 문제 조회
    Optional<Problem> findById(Long problemId);

    // 문제 제목으로 문제 조회
    Optional<Problem> findByTitle(String title);

//     문제 번호로 문제 조회
//    Optional<Problem> findByNumber(String number);
}