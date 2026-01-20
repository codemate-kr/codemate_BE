package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.team.domain.TeamJoin;
import com.ryu.studyhelper.team.domain.TeamJoinStatus;
import com.ryu.studyhelper.team.domain.TeamJoinType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamJoinRepository extends JpaRepository<TeamJoin, Long> {

    List<TeamJoin> findByTargetMemberIdAndStatus(Long targetMemberId, TeamJoinStatus status);

    List<TeamJoin> findByRequesterIdAndStatus(Long requesterId, TeamJoinStatus status);

    Optional<TeamJoin> findByTeamIdAndTargetMemberIdAndTypeAndStatus(
            Long teamId, Long targetMemberId, TeamJoinType type, TeamJoinStatus status);

    boolean existsByTeamIdAndTargetMemberIdAndTypeAndStatus(
            Long teamId, Long targetMemberId, TeamJoinType type, TeamJoinStatus status);
}