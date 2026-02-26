package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.team.repository.TeamIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.repository.SquadIncludeTagRepository;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.notification.domain.NotificationType;
import com.ryu.studyhelper.notification.service.NotificationService;
import com.ryu.studyhelper.recommendation.service.RecommendationService;
import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import com.ryu.studyhelper.team.dto.request.CreateTeamRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamInfoRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamVisibilityRequest;
import com.ryu.studyhelper.team.dto.response.CreateTeamResponse;
import com.ryu.studyhelper.team.dto.response.MyTeamResponse;
import com.ryu.studyhelper.team.dto.response.PublicTeamResponse;
import com.ryu.studyhelper.team.dto.response.TeamMemberResponse;
import com.ryu.studyhelper.team.dto.response.TeamPageResponse;
import com.ryu.studyhelper.team.dto.response.TeamPageResponseV2;
import com.ryu.studyhelper.team.dto.response.TeamRecommendationSettingsResponse;
import com.ryu.studyhelper.team.dto.response.SquadSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private static final int MAX_TEAM_CREATION_LIMIT = 3;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final SquadRepository squadRepository;
    private final SquadIncludeTagRepository squadIncludeTagRepository;
    private final TeamIncludeTagRepository teamIncludeTagRepository;
    private final RecommendationService recommendationService;
    private final NotificationService notificationService;

    @Transactional
    public CreateTeamResponse create(@Valid CreateTeamRequest req, Long memberId) {
        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        validateTeamCreationLimit(memberId);

        Team team = Team.create(req.name(), req.description(), req.isPrivate());
        Team saved = teamRepository.save(team);

        Squad defaultSquad = squadRepository.save(Squad.createDefault(saved));
        teamMemberRepository.save(TeamMember.createLeader(saved, owner, defaultSquad.getId()));

        return CreateTeamResponse.from(saved.getName(), saved.getDescription());
    }

    @Transactional(readOnly = true)
    public List<MyTeamResponse> getMyTeams(Long memberId) {
        return teamMemberRepository.findByMemberId(memberId).stream()
                .map(MyTeamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicTeamResponse> getPublicTeams() {
        return teamRepository.findByIsPrivateFalse().stream()
                .map(PublicTeamResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(Long teamId, Long currentMemberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        Map<Long, String> squadNameMap = buildSquadNameMap(teamId);

        return team.getTeamMembers().stream()
                .map(tm -> TeamMemberResponse.from(tm, currentMemberId, squadNameMap.get(tm.getSquadId())))
                .toList();
    }

    // TODO(#172): 2차 배포 시 제거 - V1 팀 페이지, getTeamPageDetailV2로 대체
    @Transactional(readOnly = true)
    public TeamPageResponse getTeamPageDetail(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validatePrivateTeamAccess(team, memberId);

        TeamPageResponse.TeamInfo teamInfo = new TeamPageResponse.TeamInfo(
                team.getId(), team.getName(), team.getDescription(),
                team.getIsPrivate(), team.getTeamMembers().size()
        );

        Map<Long, String> squadNameMap = buildSquadNameMap(teamId);
        List<TeamMemberResponse> members = team.getTeamMembers().stream()
                .map(tm -> TeamMemberResponse.from(tm, memberId, squadNameMap.get(tm.getSquadId())))
                .toList();

        List<String> includeTags = teamIncludeTagRepository.findTagKeysByTeamId(teamId);
        TeamRecommendationSettingsResponse recommendationSettings =
                TeamRecommendationSettingsResponse.from(team, includeTags);

        TodayProblemResponse todayProblem = recommendationService
                .findTodayRecommendation(teamId, memberId)
                .orElse(null);

        return new TeamPageResponse(teamInfo, members, recommendationSettings, todayProblem);
    }

    @Transactional(readOnly = true)
    public TeamPageResponseV2 getTeamPageDetailV2(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validatePrivateTeamAccess(team, memberId);

        TeamPageResponseV2.TeamInfo teamInfo = new TeamPageResponseV2.TeamInfo(
                team.getId(), team.getName(), team.getDescription(),
                team.getIsPrivate(), team.getTeamMembers().size()
        );

        List<Squad> squads = squadRepository.findByTeamIdOrderByIdAsc(teamId);
        Map<Long, String> squadNameMap = squads.stream()
                .collect(Collectors.toMap(Squad::getId, Squad::getName));

        List<TeamMemberResponse> members = team.getTeamMembers().stream()
                .map(tm -> TeamMemberResponse.from(tm, memberId, squadNameMap.get(tm.getSquadId())))
                .toList();

        List<SquadSummaryResponse> squadSummaries = squads.stream()
                .map(squad -> SquadSummaryResponse.from(
                        squad,
                        teamMemberRepository.countByTeamIdAndSquadId(teamId, squad.getId()),
                        squadIncludeTagRepository.findTagKeysBySquadId(squad.getId()),
                        recommendationService.findTodayRecommendationBySquad(teamId, squad.getId(), memberId).orElse(null)
                ))
                .toList();

        return new TeamPageResponseV2(teamInfo, members, squadSummaries);
    }

    @Transactional
    public void updateTeamInfo(Long teamId, UpdateTeamInfoRequest request, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateTeamLeaderAccess(teamId, memberId);
        team.updateInfo(request.name(), request.description(), request.isPrivate());
        teamRepository.save(team);
    }

    @Transactional
    public void updateVisibility(Long teamId, UpdateTeamVisibilityRequest request, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateTeamLeaderAccess(teamId, memberId);
        team.updateVisibility(request.isPrivate());
        teamRepository.save(team);
    }

    @Transactional
    public void leaveTeam(Long teamId, Long memberId) {
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_MEMBER_NOT_FOUND));

        if (teamMember.getRole() == TeamRole.LEADER) {
            throw new CustomException(CustomResponseStatus.TEAM_LEADER_CANNOT_LEAVE);
        }

        Team team = teamMember.getTeam();
        Member member = teamMember.getMember();

        teamMemberRepository.delete(teamMember);

        teamMemberRepository.findLeaderIdByTeamId(teamId).ifPresent(leaderId ->
                notificationService.createNotification(
                        leaderId,
                        NotificationType.MEMBER_LEFT,
                        Map.of(
                                "teamId", team.getId(),
                                "teamName", team.getName(),
                                "memberId", member.getId(),
                                "memberName", member.getHandle()
                        )
                )
        );
    }

    @Transactional
    public void deleteTeam(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateTeamLeaderAccess(teamId, memberId);

        // TeamIncludeTag → SquadIncludeTag → Squad → Team 순서로 삭제
        teamIncludeTagRepository.deleteAllByTeamId(teamId);
        List<Squad> squads = squadRepository.findByTeamIdOrderByIdAsc(teamId);
        squads.forEach(squad -> squadIncludeTagRepository.deleteAllBySquadId(squad.getId()));
        squadRepository.deleteAll(squads);

        teamRepository.delete(team);
    }

    /**
     * SpEL에서 사용: @teamService.isTeamLeader(#teamId, authentication.principal.memberId)
     */
    public boolean isTeamLeader(Long teamId, Long memberId) {
        return teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER);
    }

    private void validateTeamLeaderAccess(Long teamId, Long memberId) {
        if (!isTeamLeader(teamId, memberId)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }

    private void validateTeamCreationLimit(Long memberId) {
        if (teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER) >= MAX_TEAM_CREATION_LIMIT) {
            throw new CustomException(CustomResponseStatus.TEAM_CREATION_LIMIT_EXCEEDED);
        }
    }

    public void validateTeamAccess(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));
        validatePrivateTeamAccess(team, memberId);
    }

    private void validatePrivateTeamAccess(Team team, Long memberId) {
        if (!team.getIsPrivate()) {
            return;
        }
        if (memberId == null || !teamMemberRepository.existsByTeamIdAndMemberId(team.getId(), memberId)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }

    private Map<Long, String> buildSquadNameMap(Long teamId) {
        return squadRepository.findByTeamIdOrderByIdAsc(teamId).stream()
                .collect(Collectors.toMap(Squad::getId, Squad::getName));
    }
}
