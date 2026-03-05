package com.ryu.studyhelper.recommendation.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 추천 배치 (팀과 독립적 관리, soft delete)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "recommendation")
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "varchar(16)")
    private RecommendationType type;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "squad_id")
    private Long squadId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(16)")
    private RecommendationStatus status;

    @OneToMany(mappedBy = "recommendation")
    @Builder.Default
    private List<RecommendationProblem> problems = new ArrayList<>();

    /**
     * PENDING 상태로 새 추천 레코드 생성
     */
    public static Recommendation createPending(Long teamId, Long squadId, RecommendationType type, LocalDate date) {
        return Recommendation.builder()
                .teamId(teamId)
                .squadId(squadId)
                .type(type)
                .date(date)
                .status(RecommendationStatus.PENDING)
                .build();
    }

    public void updateStatus(RecommendationStatus status) {
        this.status = status;
    }
}
