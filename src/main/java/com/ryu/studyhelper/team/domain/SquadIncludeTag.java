package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.problem.domain.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "squad_include_tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_squad_include_tag",
                columnNames = {"squad_id", "tag_key"}
        ),
        indexes = {
                @Index(name = "idx_squad_include_tag_squad", columnList = "squad_id"),
                @Index(name = "idx_squad_include_tag_tag", columnList = "tag_key")
        })
public class SquadIncludeTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_key", nullable = false)
    private Tag tag;

    public static SquadIncludeTag create(Squad squad, Tag tag) {
        return SquadIncludeTag.builder()
                .squad(squad)
                .tag(tag)
                .build();
    }
}
