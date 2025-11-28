package com.ryu.studyhelper.team;

import com.ryu.studyhelper.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * 모든 공개 팀 목록 조회
     * @return 공개 팀 목록
     */
    List<Team> findByIsPrivateFalse();
}
