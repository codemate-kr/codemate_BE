package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TeamMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 팀 내 역할 (LEADER: 리더, MEMBER: 팀원)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role;

    @Column(name = "squad_id")
    private Long squadId;

    /**
     * 팀원 생성을 위한 팩토리 메서드
     */
    public static TeamMember create(Team team, Member member, TeamRole role, Long squadId) {
        return TeamMember.builder()
                .team(team)
                .member(member)
                .role(role)
                .squadId(squadId)
                .build();
    }

    public static TeamMember create(Team team, Member member, TeamRole role) {
        return create(team, member, role, null);
    }

    /**
     * 리더 생성을 위한 팩토리 메서드
     */
    public static TeamMember createLeader(Team team, Member member) {
        return create(team, member, TeamRole.LEADER);
    }

    public static TeamMember createLeader(Team team, Member member, Long squadId) {
        return create(team, member, TeamRole.LEADER, squadId);
    }

    /**
     * 일반 팀원 생성을 위한 팩토리 메서드
     */
    public static TeamMember createMember(Team team, Member member) {
        return create(team, member, TeamRole.MEMBER);
    }

    public static TeamMember createMember(Team team, Member member, Long squadId) {
        return create(team, member, TeamRole.MEMBER, squadId);
    }

    public void updateSquadId(Long squadId) {
        this.squadId = squadId;
    }
}
