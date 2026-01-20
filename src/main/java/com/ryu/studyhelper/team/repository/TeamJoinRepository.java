package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.team.domain.TeamJoin;
import com.ryu.studyhelper.team.domain.TeamJoinStatus;
import com.ryu.studyhelper.team.domain.TeamJoinType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeamJoinRepository extends JpaRepository<TeamJoin, Long> {

    List<TeamJoin> findByTargetMemberIdAndStatusAndExpiresAtAfter(
            Long targetMemberId, TeamJoinStatus status, LocalDateTime now);

    List<TeamJoin> findByRequesterIdAndStatusAndExpiresAtAfter(
            Long requesterId, TeamJoinStatus status, LocalDateTime now);

    Optional<TeamJoin> findByTeamIdAndTargetMemberIdAndTypeAndStatus(
            Long teamId, Long targetMemberId, TeamJoinType type, TeamJoinStatus status);

    boolean existsByTeamIdAndTargetMemberIdAndTypeAndStatus(
            Long teamId, Long targetMemberId, TeamJoinType type, TeamJoinStatus status);
}