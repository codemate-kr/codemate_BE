package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import com.ryu.studyhelper.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_join", indexes = {
        @Index(name = "idx_target_status", columnList = "target_member_id, status"),
        @Index(name = "idx_team_status", columnList = "team_id, status"),
        @Index(name = "idx_requester_status", columnList = "requester_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TeamJoin extends BaseEntity {

    private static final long INVITATION_EXPIRE_DAYS = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(16)")
    private TeamJoinType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_member_id")
    private Member targetMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(16)")
    @Builder.Default
    private TeamJoinStatus status = TeamJoinStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static TeamJoin createInvitation(Team team, Member requester, Member targetMember) {
        return TeamJoin.builder()
                .team(team)
                .type(TeamJoinType.INVITATION)
                .requester(requester)
                .targetMember(targetMember)
                .status(TeamJoinStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_EXPIRE_DAYS))
                .build();
    }

    public static TeamJoin createApplication(Team team, Member applicant) {
        return TeamJoin.builder()
                .team(team)
                .type(TeamJoinType.APPLICATION)
                .requester(applicant)
                .targetMember(null)
                .status(TeamJoinStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_EXPIRE_DAYS))
                .build();
    }

    public void accept() {
        this.status = TeamJoinStatus.ACCEPTED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = TeamJoinStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TeamJoinStatus.CANCELED;
        this.processedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == TeamJoinStatus.PENDING;
    }
}