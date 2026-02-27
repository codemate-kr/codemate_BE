package com.ryu.studyhelper.team.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 팀 이름
    @Column(nullable = false)
    private String name;

    // 팀 설명
    private String description;

    // 팀 공개/비공개 설정 (false: 공개, true: 비공개)
    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    // 팀원 목록 (일대다 관계)
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMember> teamMembers = new ArrayList<>();

    /**
     * 팀 생성을 위한 팩토리 메서드
     * @param name 팀 이름
     * @param description 팀 설명
     * @param isPrivate 비공개 팀 여부 (true: 비공개, false: 공개)
     */
    public static Team create(String name, String description, Boolean isPrivate) {
        return Team.builder()
                .name(name)
                .description(description)
                .isPrivate(isPrivate != null ? isPrivate : false)
                .build();
    }

    /**
     * 팀 공개/비공개 설정 변경
     * @param isPrivate true: 비공개, false: 공개
     */
    public void updateVisibility(Boolean isPrivate) {
        this.isPrivate = isPrivate != null ? isPrivate : false;
    }

    /**
     * 팀 기본 정보 수정
     * @param name 팀 이름
     * @param description 팀 설명
     * @param isPrivate 비공개 여부 (null이면 기존 값 유지)
     */
    public void updateInfo(String name, String description, Boolean isPrivate) {
        this.name = name;
        this.description = description;
        if (isPrivate != null) {
            this.isPrivate = isPrivate;
        }
    }
}
