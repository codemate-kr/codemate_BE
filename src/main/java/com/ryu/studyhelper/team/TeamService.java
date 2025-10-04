package com.ryu.studyhelper.team;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.dto.CreateTeamRequest;
import com.ryu.studyhelper.team.dto.CreateTeamResponse;
import com.ryu.studyhelper.team.dto.MyTeamResponse;
import com.ryu.studyhelper.team.dto.TeamMemberResponse;
import com.ryu.studyhelper.team.dto.TeamRecommendationSettingsRequest;
import com.ryu.studyhelper.team.dto.TeamRecommendationSettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public CreateTeamResponse create(@Valid CreateTeamRequest req, Long memberId) {
        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        Team team = Team.create(req.name(), req.description());
        Team saved = teamRepository.save(team);

        // 현재 로그인한 사용자를 TeamMember로 자동 합류(LEADER)
        TeamMember leader = TeamMember.createLeader(saved, owner);
        teamMemberRepository.save(leader);

        return CreateTeamResponse.from(
                saved.getName(),
                saved.getDescription()
        );
    }

    /**
     * 팀 가입
     * @param teamId 팀 ID
     * @param memberId 회원 ID
     */
    @Transactional
    public void joinTeam(Long teamId, Long memberId) {
        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        // 이미 팀에 속해있는지 확인
        boolean alreadyMember = teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId);
        if (alreadyMember) {
            throw new CustomException(CustomResponseStatus.ALREADY_MAP_EXIST);
        }

        // 팀 가입
        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .member(member)
                .role(TeamRole.MEMBER)
                .build();

        teamMemberRepository.save(teamMember);
    }

    /**
     * 팀별 추천 설정 조회
     */
    @Transactional(readOnly = true)
    public TeamRecommendationSettingsResponse getRecommendationSettings(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));
        
        return TeamRecommendationSettingsResponse.from(team);
    }

    /**
     * 팀별 추천 설정 업데이트 (팀장만 가능)
     */
    @Transactional
    public TeamRecommendationSettingsResponse updateRecommendationSettings(
            Long teamId,
            TeamRecommendationSettingsRequest request,
            Long memberId) {

        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 팀장 권한 확인
        validateTeamLeaderAccess(teamId, memberId);

        // 커스텀 모드일 때 난이도 범위 유효성 검증
        if (request.problemDifficultyPreset().isCustom()) {
            if (request.customMinLevel() != null && request.customMaxLevel() != null
                && request.customMinLevel() > request.customMaxLevel()) {
                throw new CustomException(CustomResponseStatus.INVALID_PROBLEM_LEVEL_RANGE);
            }
        }

        // 추천 요일 설정 업데이트
        team.updateRecommendationDays(request.recommendationDays());

        // 추천 난이도 설정 업데이트 (프리셋 방식)
        team.updateProblemDifficultySettings(
                request.problemDifficultyPreset(),
                request.customMinLevel(),
                request.customMaxLevel()
        );

        return TeamRecommendationSettingsResponse.from(team);
    }

    /**
     * 팀별 추천 비활성화 (팀장만 가능)
     */
    @Transactional
    public TeamRecommendationSettingsResponse disableRecommendation(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));
        
        validateTeamLeaderAccess(teamId, memberId);
        
        // 빈 Set으로 설정하여 비활성화
        team.updateRecommendationDays(Set.of());
        
        return TeamRecommendationSettingsResponse.from(team);
    }

    /**
     * 사용자가 속한 팀 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MyTeamResponse> getMyTeams(Long memberId) {
        List<TeamMember> teamMembers = teamMemberRepository.findByMemberId(memberId);

        return teamMembers.stream()
                .map(MyTeamResponse::from)
                .toList();
    }

    /**
     * 팀 멤버 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(Long teamId, Long currentMemberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        return team.getTeamMembers().stream()
                .map(teamMember -> TeamMemberResponse.from(teamMember, currentMemberId))
                .toList();
    }

    /**
     * 팀장 권한 확인
     */
    private void validateTeamLeaderAccess(Long teamId, Long memberId) {
        boolean isLeader = teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER);
        if (!isLeader) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }

    /**
     * 팀장 여부 확인 (SpEL에서 사용)
     */
    public boolean isTeamLeader(Long teamId, Long memberId) {
        return teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER);
    }
}