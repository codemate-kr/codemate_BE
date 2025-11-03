package com.ryu.studyhelper.team;

import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query("""
      select m.handle
      from TeamMember tm join tm.member m
      where tm.team.id = :teamId
    """)
    List<String> findHandlesByTeamId(Long teamId);

    @Query("""
           select m.email
           from TeamMember tm
           join tm.member m
           where tm.team.id = :teamId
             and m.email is not null
             and m.email <> ''
           """)
    List<String> findEmailsByTeamId(Long teamId);

    /**
     * 특정 팀에 특정 멤버가 속해있는지 확인
     */
    boolean existsByTeamIdAndMemberId(Long teamId, Long memberId);

    /**
     * 특정 팀에서 특정 멤버가 특정 역할을 가지고 있는지 확인
     */
    boolean existsByTeamIdAndMemberIdAndRole(Long teamId, Long memberId, TeamRole role);

    /**
     * 특정 멤버가 속한 모든 팀 멤버십 조회
     */
    List<TeamMember> findByMemberId(Long memberId);

    /**
     * 특정 팀에서 특정 멤버의 팀 멤버십 조회
     */
    Optional<TeamMember> findByTeamIdAndMemberId(Long teamId, Long memberId);

    /**
     * 특정 멤버가 LEADER 역할로 속한 팀의 개수 조회
     */
    int countByMemberIdAndRole(Long memberId, TeamRole role);
}
