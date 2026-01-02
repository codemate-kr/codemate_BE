package com.ryu.studyhelper.problem.domain;

import com.ryu.studyhelper.common.util.ProblemUrlUtils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    public static Problem create(Long id, String title, Integer level,
                                 Integer acceptedUserCount, Double averageTries) {
        return Problem.builder()
                .id(id)
                .title(title)
                .titleKo(title)
                .level(level)
                .acceptedUserCount(acceptedUserCount)
                .averageTries(averageTries)
                .build();
    }

    public void updateMetadata(String title, Integer level,
                               Integer acceptedUserCount, Double averageTries) {
        this.title = title;
        this.titleKo = title;
        this.level = level;
        this.acceptedUserCount = acceptedUserCount;
        this.averageTries = averageTries;
    }

    public String getUrl() {
        return ProblemUrlUtils.generateProblemUrl(id);
    }
}