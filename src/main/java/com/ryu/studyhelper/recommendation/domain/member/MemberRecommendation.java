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
 * 개인-추천 연결 엔티티 (이메일 발송 관리)
 * 팀원 개개인에게 추천이 배정되고, 이메일 발송 상태를 개인별로 관리합니다.
 *
 * 핵심 역할:
 * 1. Member와 Recommendation을 연결 (M:N 관계)
 * 2. 개인별 이메일 발송 상태 관리 (PENDING/SENT/FAILED)
 * 3. 이메일 발송 실패 시 재시도 가능
 *
 * AS-IS: TeamRecommendation.status로 팀 단위 발송 상태만 관리
 * TO-BE: MemberRecommendation.emailSendStatus로 개인별 발송 상태 관리
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

    /**
     * 회원 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 추천 배치 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    /**
     * 이메일 발송 상태 (PENDING/SENT/FAILED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "email_send_status", length = 16, nullable = false)
    private EmailSendStatus emailSendStatus;

    /**
     * 이메일 발송 시각 (발송 성공 시에만 기록)
     */
    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    /**
     * 개인별 추천 문제들 (1:N)
     */
    @OneToMany(mappedBy = "memberRecommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberRecommendationProblem> problems = new ArrayList<>();

    /**
     * 개인 추천 생성을 위한 팩토리 메서드
     * 초기 상태는 PENDING입니다.
     *
     * @param member 추천을 받을 회원
     * @param recommendation 추천 배치
     * @return 생성된 MemberRecommendation 엔티티
     */
    public static MemberRecommendation create(Member member, Recommendation recommendation) {
        return MemberRecommendation.builder()
                .member(member)
                .recommendation(recommendation)
                .emailSendStatus(EmailSendStatus.PENDING)
                .build();
    }

    /**
     * 개인 추천 문제 추가
     * 양방향 관계 설정을 위해 MemberRecommendationProblem에도 this를 세팅합니다.
     *
     * @param problem 추가할 문제
     */
    public void addProblem(MemberRecommendationProblem problem) {
        problems.add(problem);
        problem.setMemberRecommendation(this);
    }

    /**
     * 이메일 발송 완료 처리
     */
    public void markEmailAsSent() {
        this.emailSendStatus = EmailSendStatus.SENT;
        this.emailSentAt = LocalDateTime.now();
    }

    /**
     * 이메일 발송 실패 처리
     */
    public void markEmailAsFailed() {
        this.emailSendStatus = EmailSendStatus.FAILED;
    }
}