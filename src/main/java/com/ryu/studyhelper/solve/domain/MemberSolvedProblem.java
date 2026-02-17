package com.ryu.studyhelper.solve.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member_solved_problem",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_problem",
                columnNames = {"member_id", "problem_id"}
        ))
public class MemberSolvedProblem  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    private LocalDateTime solvedAt;

    public static MemberSolvedProblem create(Member member, Problem problem) {
        return MemberSolvedProblem.builder()
                .member(member)
                .problem(problem)
                .solvedAt(LocalDateTime.now())
                .build();
    }
}
