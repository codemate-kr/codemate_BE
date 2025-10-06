package com.ryu.studyhelper.problem.domain;

import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Problem {

    @Id
    private Long id;  // problemId

    @Column(nullable = false)
    private String title;

    @Column(name = "title_ko")
    private String titleKo;

    private Integer level;

    private Integer acceptedUserCount;

    private Double averageTries;

    /**
     * 기존 팩토리 메서드 (하위 호환성 유지)
     */
    public static Problem of(Long id, String title, Integer level, Integer acceptedUserCount) {
        return Problem.builder()
                .id(id)
                .title(title)
                .titleKo(title) // 기본값으로 title 사용
                .level(level)
                .acceptedUserCount(acceptedUserCount)
                .build();
    }

    /**
     * ProblemInfo로부터 Problem 엔티티 생성
     */
    public static Problem from(ProblemInfo problemInfo) {
        return Problem.builder()
                .id(problemInfo.problemId())
                .title(problemInfo.titleKo()) // ProblemInfo에는 title이 없으므로 titleKo 사용
                .titleKo(problemInfo.titleKo())
                .level(problemInfo.level())
                .acceptedUserCount(problemInfo.acceptedUserCount())
                .averageTries(problemInfo.averageTries())
                .build();
    }

    /**
     * 백준 문제 URL 생성
     */
    public String getUrl() {
        return "https://www.acmicpc.net/problem/" + id;
    }
}