package com.ryu.studyhelper.recommendation.domain.member;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 개인-추천 연결 (이메일 발송 상태 관리)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member_recommendation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_recommendation",
                columnNames = {"member_id", "recommendation_id"}
        ))
public class MemberRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @Column(name = "team_id")
    private Long teamId;

    /**
     * denormalized - Team JOIN 없이 조회
     */
    @Column(name = "team_name")
    private String teamName;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_send_status", length = 16, nullable = false)
    private EmailSendStatus emailSendStatus;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @OneToMany(mappedBy = "memberRecommendation")
    @Builder.Default
    private List<MemberRecommendationProblem> problems = new ArrayList<>();

    public static MemberRecommendation create(Member member, Recommendation recommendation) {
        return MemberRecommendation.builder()
                .member(member)
                .recommendation(recommendation)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
    }

    /**
     * 양방향 연관관계 편의 메서드
     */
    public void addProblem(MemberRecommendationProblem problem) {
        problems.add(problem);
        problem.setMemberRecommendation(this);
    }

    public void markEmailAsSent() {
        this.emailSendStatus = EmailSendStatus.SENT;
        this.emailSentAt = LocalDateTime.now();
    }

    public void markEmailAsFailed() {
        this.emailSendStatus = EmailSendStatus.FAILED;
    }
}