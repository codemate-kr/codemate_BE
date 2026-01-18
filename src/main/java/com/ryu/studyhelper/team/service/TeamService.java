package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.repository.TagRepository;
import com.ryu.studyhelper.problem.domain.Tag;
import com.ryu.studyhelper.team.repository.TeamIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamIncludeTag;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.recommendation.RecommendationService;
import com.ryu.studyhelper.recommendation.dto.response.TodayProblemResponse;
import com.ryu.studyhelper.team.dto.request.CreateTeamRequest;
import com.ryu.studyhelper.team.dto.request.InviteMemberRequest;
import com.ryu.studyhelper.team.dto.request.TeamRecommendationSettingsRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamInfoRequest;
import com.ryu.studyhelper.team.dto.request.UpdateTeamVisibilityRequest;
import com.ryu.studyhelper.team.dto.response.CreateTeamResponse;
import com.ryu.studyhelper.team.dto.response.InviteMemberResponse;
import com.ryu.studyhelper.team.dto.response.MyTeamResponse;
import com.ryu.studyhelper.team.dto.response.PublicTeamResponse;
import com.ryu.studyhelper.team.dto.response.TeamMemberResponse;
import com.ryu.studyhelper.team.dto.response.TeamPageResponse;
import com.ryu.studyhelper.team.dto.response.TeamRecommendationSettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final RecommendationService recommendationService;
    private final TeamIncludeTagRepository teamIncludeTagRepository;
    private final TagRepository tagRepository;
    // TODO: 알림 시스템 구현 후 주입
    // private final NotificationService notificationService;

    /**
     * 팀 생성 제한 상수 (LEADER 역할로 생성 가능한 최대 팀 개수)
     */
    private static final int MAX_TEAM_CREATION_LIMIT = 3;


    @Transactional
    public CreateTeamResponse create(@Valid CreateTeamRequest req, Long memberId) {
        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        // 팀 생성 제한 검증 (LEADER 역할 최대 3개)
        validateTeamCreationLimit(memberId);

        Team team = Team.create(req.name(), req.description(), req.isPrivate());
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
     * 팀 가입 (공개 팀만 가능)
     * - 비공개 팀은 초대를 통해서만 가입 가능
     * @param teamId 팀 ID
     * @param memberId 회원 ID
     */
    @Transactional
    public void joinTeam(Long teamId, Long memberId) {
        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 비공개 팀은 직접 가입 불가 (초대만 가능)
        if (team.getIsPrivate()) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        // 이미 팀에 속해있는지 확인
        boolean alreadyMember = teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId);
        if (alreadyMember) {
            throw new CustomException(CustomResponseStatus.ALREADY_TEAM_MEMBER);
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

        List<String> includeTags = teamIncludeTagRepository.findTagKeysByTeamId(teamId);
        return TeamRecommendationSettingsResponse.from(team, includeTags);
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
            if (request.minProblemLevel() != null && request.maxProblemLevel() != null
                && request.minProblemLevel() > request.maxProblemLevel()) {
                throw new CustomException(CustomResponseStatus.INVALID_PROBLEM_LEVEL_RANGE);
            }
        }

        // 추천 요일 설정 업데이트
        team.updateRecommendationDays(request.recommendationDays());

        // 추천 난이도 설정 업데이트 (프리셋 방식)
        team.updateProblemDifficultySettings(
                request.problemDifficultyPreset(),
                request.minProblemLevel(),
                request.maxProblemLevel()
        );

        // 추천 문제 개수 업데이트
        team.updateProblemCount(request.problemCount());

        // 포함 태그 설정 업데이트
        List<String> updatedTags = updateIncludeTags(team, request.includeTags());

        return TeamRecommendationSettingsResponse.from(team, updatedTags);
    }

    /**
     * 팀별 추천 비활성화 (팀장만 가능)
     */
    @Transactional
    public TeamRecommendationSettingsResponse disableRecommendation(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));
        
        validateTeamLeaderAccess(teamId, memberId);

        // 빈 List로 설정하여 비활성화
        team.updateRecommendationDays(List.of());

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
     * 모든 공개 팀 목록 조회
     * - 비로그인 사용자도 조회 가능
     * @return 공개 팀 목록
     */
    @Transactional(readOnly = true)
    public List<PublicTeamResponse> getPublicTeams() {
        List<Team> publicTeams = teamRepository.findByIsPrivateFalse();

        return publicTeams.stream()
                .map(PublicTeamResponse::from)
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
     * 팀 페이지 통합 조회 (비공개 팀 권한 체크 포함)
     * - 팀 기본 정보, 멤버 목록, 추천 설정, 오늘의 문제를 한번에 조회
     * - 비공개 팀인 경우 팀원만 접근 가능
     * @param teamId 팀 ID
     * @param memberId 현재 로그인한 멤버 ID (비로그인 시 null)
     */
    @Transactional(readOnly = true)
    public TeamPageResponse getTeamPageDetail(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 비공개 팀 접근 권한 검증
        validatePrivateTeamAccess(team, memberId);

        // 팀 기본 정보
        TeamPageResponse.TeamInfo teamInfo = new TeamPageResponse.TeamInfo(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getIsPrivate(),
                team.getTeamMembers().size()
        );

        // 팀 멤버 목록
        List<TeamMemberResponse> members = team.getTeamMembers().stream()
                .map(teamMember -> TeamMemberResponse.from(teamMember, memberId))
                .toList();

        // 추천 설정 (태그 포함)
        List<String> includeTags = teamIncludeTagRepository.findTagKeysByTeamId(teamId);
        TeamRecommendationSettingsResponse recommendationSettings = TeamRecommendationSettingsResponse.from(team, includeTags);

        // 오늘의 문제 (없으면 null)
        TodayProblemResponse todayProblem = recommendationService
                .findTodayRecommendation(teamId, memberId)
                .orElse(null);

        return new TeamPageResponse(teamInfo, members, recommendationSettings, todayProblem);
    }

    /**
     * 비공개 팀 접근 권한 검증
     * - 공개 팀: 누구나 접근 가능
     * - 비공개 팀: 팀원만 접근 가능
     */
    private void validatePrivateTeamAccess(Team team, Long memberId) {
        if (!team.getIsPrivate()) {
            return; // 공개 팀은 누구나 접근 가능
        }

        // 비공개 팀: 로그인하지 않았거나 팀원이 아니면 접근 불가
        if (memberId == null) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        boolean isMember = teamMemberRepository.existsByTeamIdAndMemberId(team.getId(), memberId);
        if (!isMember) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
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

    /**
     * 팀 생성 가능 여부 검증 (LEADER 역할 최대 3개 제한)
     */
    private void validateTeamCreationLimit(Long memberId) {
        int leaderCount = teamMemberRepository.countByMemberIdAndRole(memberId, TeamRole.LEADER);
        if (leaderCount >= MAX_TEAM_CREATION_LIMIT) {
            throw new CustomException(CustomResponseStatus.TEAM_CREATION_LIMIT_EXCEEDED);
        }
    }

    /**
     * 멤버 초대 (팀장만 가능)
     * @param teamId 팀 ID
     * @param request 초대 요청 (memberId)
     * @param currentMemberId 현재 로그인한 멤버 ID (팀장)
     * @return 초대 결과
     */
    @Transactional
    public InviteMemberResponse inviteMember(Long teamId, InviteMemberRequest request, Long currentMemberId) {
        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 팀장 권한 확인
        validateTeamLeaderAccess(teamId, currentMemberId);

        // 초대할 멤버 조회
        Member invitedMember = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        // 이미 팀에 속해있는지 확인
        boolean alreadyMember = teamMemberRepository.existsByTeamIdAndMemberId(teamId, invitedMember.getId());
        if (alreadyMember) {
            throw new CustomException(CustomResponseStatus.ALREADY_TEAM_MEMBER);
        }

        // 팀에 멤버 추가
        TeamMember teamMember = TeamMember.createMember(team, invitedMember);
        TeamMember savedTeamMember = teamMemberRepository.save(teamMember);

        // TODO: 웹서비스 자체 알림 시스템을 통해 초대 알림 발송
        // 알림 서비스가 구현되면 여기서 호출
        // notificationService.sendTeamInvitationNotification(invitedMember.getId(), team.getId(), team.getName());

        return InviteMemberResponse.from(savedTeamMember);
    }

    /**
     * 팀 탈퇴 (일반 멤버만 가능, 리더는 불가)
     * @param teamId 팀 ID
     * @param memberId 현재 로그인한 멤버 ID
     */
    @Transactional
    public void leaveTeam(Long teamId, Long memberId) {

        // 팀 멤버십 조회
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_MEMBER_NOT_FOUND));

        // 리더는 탈퇴 불가
        if (teamMember.getRole() == TeamRole.LEADER) {
            throw new CustomException(CustomResponseStatus.TEAM_LEADER_CANNOT_LEAVE);
        }

        // 팀 멤버십 삭제
        teamMemberRepository.delete(teamMember);
    }

    /**
     * 팀 삭제 (리더만 가능)
     * 팀과 연관된 모든 데이터(팀 멤버, 추천 기록 등)가 함께 삭제됩니다.
     * @param teamId 팀 ID
     * @param memberId 현재 로그인한 멤버 ID (리더)
     */
    @Transactional
    public void deleteTeam(Long teamId, Long memberId) {
        // 팀 조회
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 팀장 권한 확인
        validateTeamLeaderAccess(teamId, memberId);

        // 팀 삭제 (cascade로 연관된 TeamMember, TeamRecommendation 등도 함께 삭제됨)
        teamRepository.delete(team);
    }

    /**
     * 팀 공개/비공개 설정 변경 (팀장만 가능)
     * @param teamId 팀 ID
     * @param request 공개/비공개 설정 요청
     * @param memberId 현재 로그인한 멤버 ID (팀장)
     */
    @Transactional
    public void updateVisibility(Long teamId, UpdateTeamVisibilityRequest request, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateTeamLeaderAccess(teamId, memberId);

        team.updateVisibility(request.isPrivate());
    }

    /**
     * 팀 정보 수정 (팀장만 가능)
     * @param teamId 팀 ID
     * @param request 팀 정보 수정 요청 (이름, 설명, 공개/비공개)
     * @param memberId 현재 로그인한 멤버 ID (팀장)
     */
    @Transactional
    public void updateTeamInfo(Long teamId, UpdateTeamInfoRequest request, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateTeamLeaderAccess(teamId, memberId);

        team.updateInfo(request.name(), request.description(), request.isPrivate());
    }

    /**
     * 팀의 포함 태그 설정 업데이트
     * - 기존 태그 전체 삭제 후 새 태그 추가 (Replace All 전략)
     * @param team 팀 엔티티
     * @param tagKeys 설정할 태그 키 목록 (null이면 빈 목록으로 처리)
     * @return 업데이트된 태그 키 목록
     */
    private List<String> updateIncludeTags(Team team, List<String> tagKeys) {
        // null이면 빈 목록으로 처리
        List<String> keys = tagKeys != null ? tagKeys : List.of();

        // 기존 태그 전체 삭제
        teamIncludeTagRepository.deleteAllByTeamId(team.getId());

        // 새 태그가 없으면 빈 목록 반환
        if (keys.isEmpty()) {
            return List.of();
        }

        // 유효한 태그만 필터링 (존재하지 않는 태그 키는 무시)
        List<Tag> validTags = tagRepository.findByKeyIn(keys);

        // 무효한 태그 키가 있으면 경고 로그
        if (validTags.size() != keys.size()) {
            Set<String> validKeys = validTags.stream()
                    .map(Tag::getKey)
                    .collect(Collectors.toSet());
            List<String> invalidKeys = keys.stream()
                    .filter(k -> !validKeys.contains(k))
                    .toList();
            log.warn("팀 {} 태그 설정 시 무효한 태그 키 무시됨: {}", team.getId(), invalidKeys);
        }

        // TeamIncludeTag 엔티티 생성 및 저장
        List<TeamIncludeTag> teamIncludeTags = validTags.stream()
                .map(tag -> TeamIncludeTag.builder()
                        .team(team)
                        .tag(tag)
                        .build())
                .toList();

        teamIncludeTagRepository.saveAll(teamIncludeTags);

        // 저장된 태그 키 목록 반환
        return validTags.stream()
                .map(Tag::getKey)
                .toList();
    }

    // TODO: 알림 시스템 구현 후 활성화
    // /**
    //  * 초대 알림 발송
    //  */
    // private void sendInvitationNotification(Long memberId, Long teamId, String teamName) {
    //     // 웹서비스 자체 알림 시스템을 통해 알림 발송
    //     // notificationService.createNotification(memberId, NotificationType.TEAM_INVITATION, teamId, teamName);
    // }
}