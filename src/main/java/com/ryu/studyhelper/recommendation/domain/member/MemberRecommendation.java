package com.ryu.studyhelper.recommendation.domain.member;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.team.domain.Team;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "squad_id")
    private Long squadId;

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

    public static MemberRecommendation createForSquad(Member member, Recommendation recommendation, Team team, Long squadId) {
        if (team == null) {
            throw new IllegalArgumentException("Team must not be null");
        }
        return MemberRecommendation.builder()
                .member(member)
                .recommendation(recommendation)
                .teamId(team.getId())
                .teamName(team.getName())
                .squadId(squadId)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
    }

    public void markEmailAsSent() {
        this.emailSendStatus = EmailSendStatus.SENT;
        this.emailSentAt = LocalDateTime.now();
    }

    public void markEmailAsFailed() {
        this.emailSendStatus = EmailSendStatus.FAILED;
    }

    // 재시도 CAS 선점 후 in-memory 상태 동기화용
    public void retryAsPending() {
        if (this.emailSendStatus != EmailSendStatus.FAILED) {
            throw new IllegalStateException("FAILED 상태에서만 재시도 대기로 전이할 수 있습니다. 현재: " + this.emailSendStatus);
        }
        this.emailSendStatus = EmailSendStatus.PENDING;
        this.emailSentAt = null;
    }
}
