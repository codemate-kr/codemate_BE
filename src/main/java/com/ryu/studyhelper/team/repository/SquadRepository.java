package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.team.domain.Squad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findByTeamIdOrderByIdAsc(Long teamId);

    Optional<Squad> findByIdAndTeamId(Long squadId, Long teamId);

    Optional<Squad> findByTeamIdAndIsDefaultTrue(Long teamId);

    long countByTeamId(Long teamId);

    boolean existsByTeamIdAndName(Long teamId, String name);
}
