package com.ryu.studyhelper.recommendation.domain.team;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.team.domain.Team;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 팀별 문제 추천 이력을 저장하는 엔티티
 * 매일 자동 추천 또는 수동 추천 결과를 기록
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "team_recommendation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_recommendation_date",
                columnNames = {"team_id", "recommendation_date", "type"}
        ))
public class TeamRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(length=16, nullable = false)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status;

    @Column(name = "recommendation_date", nullable = false)
    private LocalDate recommendationDate;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @OneToMany(mappedBy = "teamRecommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamRecommendationProblem> problems = new ArrayList<>();

    /**
     * 팀별 스케줄 추천 생성을 위한 팩토리 메서드
     */
    public static TeamRecommendation createScheduledRecommendation(Team team, LocalDate date) {
        return TeamRecommendation.builder()
                .team(team)
                .type(RecommendationType.SCHEDULED)
                .status(RecommendationStatus.PENDING)
                .recommendationDate(date)
                .build();
    }

    /**
     * 수동 추천 생성을 위한 팩토리 메서드
     */
    public static TeamRecommendation createManualRecommendation(Team team) {
        return TeamRecommendation.builder()
                .team(team)
                .type(RecommendationType.MANUAL)
                .status(RecommendationStatus.PENDING)
                .recommendationDate(LocalDate.now())
                .build();
    }

    /**
     * 추천 문제 추가
     */
    public void addProblem(TeamRecommendationProblem problem) {
        problems.add(problem);
        problem.setTeamRecommendation(this);
    }

    /**
     * 이메일 발송 완료 처리
     */
    public void markAsSent() {
        this.status = RecommendationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 이메일 발송 실패 처리
     */
    public void markAsFailed() {
        this.status = RecommendationStatus.FAILED;
    }
}