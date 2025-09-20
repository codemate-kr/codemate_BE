package com.ryu.studyhelper.team;

import com.ryu.studyhelper.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
