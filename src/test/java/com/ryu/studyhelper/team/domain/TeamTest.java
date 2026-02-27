package com.ryu.studyhelper.team.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Team 도메인 테스트")
class TeamTest {

    private Team team;

    @BeforeEach
    void setUp() {
        team = Team.create("테스트팀", "알고리즘 스터디", false);
    }

    @Test
    @DisplayName("팀 생성 시 이름, 설명, 공개 설정이 저장된다")
    void create_initialState() {
        // when
        Team newTeam = Team.create("새 팀", "설명", false);

        // then
        assertThat(newTeam.getName()).isEqualTo("새 팀");
        assertThat(newTeam.getDescription()).isEqualTo("설명");
        assertThat(newTeam.getIsPrivate()).isFalse();
    }

    @Test
    @DisplayName("updateInfo로 팀 이름, 설명, 공개/비공개를 수정할 수 있다")
    void updateInfo_changesNameDescriptionAndVisibility() {
        // given
        assertThat(team.getIsPrivate()).isFalse();

        // when
        team.updateInfo("새로운 팀 이름", "새로운 설명", true);

        // then
        assertThat(team.getName()).isEqualTo("새로운 팀 이름");
        assertThat(team.getDescription()).isEqualTo("새로운 설명");
        assertThat(team.getIsPrivate()).isTrue();
    }

    @Test
    @DisplayName("updateInfo에서 isPrivate가 null이면 기존 값이 유지된다")
    void updateInfo_nullIsPrivate_keepsOriginalValue() {
        // given
        team.updateInfo("팀", "설명", true);
        assertThat(team.getIsPrivate()).isTrue();

        // when
        team.updateInfo("수정된 팀", "수정된 설명", null);

        // then
        assertThat(team.getName()).isEqualTo("수정된 팀");
        assertThat(team.getIsPrivate()).isTrue();
    }

    @Test
    @DisplayName("updateInfo로 설명을 null로 설정할 수 있다")
    void updateInfo_nullDescription() {
        // when
        team.updateInfo("팀 이름", null, false);

        // then
        assertThat(team.getName()).isEqualTo("팀 이름");
        assertThat(team.getDescription()).isNull();
    }

    @Test
    @DisplayName("updateVisibility로 공개/비공개 설정을 변경할 수 있다")
    void updateVisibility_changesPrivateSetting() {
        // given
        assertThat(team.getIsPrivate()).isFalse();

        // when
        team.updateVisibility(true);

        // then
        assertThat(team.getIsPrivate()).isTrue();
    }
}
