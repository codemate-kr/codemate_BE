package com.ryu.studyhelper.problem.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "problem_tag",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_problem_tag",
        columnNames = {"problem_id", "tag_key"}
    ),
    indexes = {
        @Index(name = "idx_problem_tag_problem", columnList = "problem_id"),
        @Index(name = "idx_problem_tag_tag", columnList = "tag_key")
    })
public class ProblemTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_key", nullable = false)
    private Tag tag;

    public static ProblemTag create(Problem problem, Tag tag) {
        return ProblemTag.builder()
                .problem(problem)
                .tag(tag)
                .build();
    }
}