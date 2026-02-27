package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.member.domain.Member;
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
      select m.handle
      from TeamMember tm join tm.member m
      where tm.team.id = :teamId
        and tm.squadId = :squadId
    """)
    List<String> findHandlesByTeamIdAndSquadId(Long teamId, Long squadId);

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
     * 특정 멤버가 어떤 팀에라도 속해있는지 확인
     */
    boolean existsByMemberId(Long memberId);

    /**
     * 특정 팀에서 특정 멤버의 팀 멤버십 조회
     */
    Optional<TeamMember> findByTeamIdAndMemberId(Long teamId, Long memberId);

    /**
     * 특정 멤버가 LEADER 역할로 속한 팀의 개수 조회
     */
    int countByMemberIdAndRole(Long memberId, TeamRole role);

    /**
     * 특정 팀의 모든 멤버 조회
     */
    @Query("""
           select tm.member
           from TeamMember tm
           where tm.team.id = :teamId
           """)
    List<Member> findMembersByTeamId(Long teamId);

    @Query("""
           select tm.member
           from TeamMember tm
           where tm.team.id = :teamId
             and tm.squadId = :squadId
           """)
    List<Member> findMembersByTeamIdAndSquadId(Long teamId, Long squadId);

    /**
     * 특정 팀의 리더 ID 조회
     */
    @Query("""
           select tm.member.id
           from TeamMember tm
           where tm.team.id = :teamId and tm.role = 'LEADER'
           """)
    Optional<Long> findLeaderIdByTeamId(Long teamId);

    List<TeamMember> findByTeamId(Long teamId);

    /**
     * 특정 팀의 모든 팀멤버 조회 (Member fetch join, N+1 방지)
     */
    @Query("""
           SELECT tm FROM TeamMember tm JOIN FETCH tm.member
           WHERE tm.team.id = :teamId
           """)
    List<TeamMember> findByTeamIdWithMember(@org.springframework.data.repository.query.Param("teamId") Long teamId);

    List<TeamMember> findByTeamIdAndSquadId(Long teamId, Long squadId);

    int countByTeamIdAndSquadId(Long teamId, Long squadId);

    /**
     * 기본 스쿼드 lazy 초기화 시 squad 미배정 멤버 조회
     */
    List<TeamMember> findByTeamIdAndSquadIdIsNull(Long teamId);
}
