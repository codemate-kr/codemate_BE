package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.problem.domain.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "team_include_tag",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_team_include_tag",
        columnNames = {"team_id", "tag_key"}
    ),
    indexes = {
        @Index(name = "idx_team_include_tag_team", columnList = "team_id"),
        @Index(name = "idx_team_include_tag_tag", columnList = "tag_key")
    })
public class TeamIncludeTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_key", nullable = false)
    private Tag tag;

    public static TeamIncludeTag create(Team team, Tag tag) {
        return TeamIncludeTag.builder()
                .team(team)
                .tag(tag)
                .build();
    }
}