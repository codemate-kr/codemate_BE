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

    /**
     * 팀원 생성을 위한 팩토리 메서드
     */
    public static TeamMember create(Team team, Member member, TeamRole role) {
        return TeamMember.builder()
                .team(team)
                .member(member)
                .role(role)
                .build();
    }

    /**
     * 리더 생성을 위한 팩토리 메서드
     */
    public static TeamMember createLeader(Team team, Member member) {
        return create(team, member, TeamRole.LEADER);
    }

    /**
     * 일반 팀원 생성을 위한 팩토리 메서드
     */
    public static TeamMember createMember(Team team, Member member) {
        return create(team, member, TeamRole.MEMBER);
    }
}