package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.notification.domain.NotificationType;
import com.ryu.studyhelper.notification.service.NotificationService;
import com.ryu.studyhelper.team.domain.*;
import com.ryu.studyhelper.team.dto.request.TeamJoinInviteRequest;
import com.ryu.studyhelper.team.dto.response.TeamJoinResponse;
import com.ryu.studyhelper.team.repository.TeamJoinRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamJoinService {

    private final TeamJoinRepository teamJoinRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final MailSendService mailSendService;
    private final NotificationService notificationService;

    @Transactional
    public TeamJoinResponse inviteMember(TeamJoinInviteRequest request, Long requesterId) {
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 팀장 권한 확인
        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRole(request.teamId(), requesterId, TeamRole.LEADER)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        Member targetMember = memberRepository.findById(request.targetMemberId())
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        // 자기 자신 초대 불가
        if (requesterId.equals(request.targetMemberId())) {
            throw new CustomException(CustomResponseStatus.CANNOT_INVITE_SELF);
        }

        // 이미 팀 멤버인지 확인
        if (teamMemberRepository.existsByTeamIdAndMemberId(request.teamId(), request.targetMemberId())) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_ALREADY_MEMBER);
        }

        // 대기 중인 초대가 있는지 확인
        if (teamJoinRepository.existsByTeamIdAndTargetMemberIdAndTypeAndStatus(
                request.teamId(), request.targetMemberId(), TeamJoinType.INVITATION, TeamJoinStatus.PENDING)) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_ALREADY_EXISTS);
        }

        TeamJoin teamJoin = TeamJoin.createInvitation(team, requester, targetMember);
        TeamJoin saved = teamJoinRepository.save(teamJoin);

        // 이메일 발송
        try {
            mailSendService.sendTeamInvitationEmail(saved);
        } catch (Exception e) {
            log.warn("초대 이메일 발송 실패 (teamJoinId={}): {}", saved.getId(), e.getMessage());
        }

        // 웹 알림 생성
        notificationService.createNotification(
                targetMember.getId(),
                NotificationType.TEAM_INVITATION,
                Map.of(
                        "teamId", team.getId(),
                        "teamName", team.getName(),
                        "inviterId", requester.getId(),
                        "inviterName", requester.getHandle()
                )
        );

        return TeamJoinResponse.from(saved);
    }

    @Transactional
    public TeamJoinResponse accept(Long teamJoinId, Long memberId) {
        TeamJoin teamJoin = findAndValidateForProcess(teamJoinId, memberId, true);

        teamJoin.accept();

        // 팀 멤버로 추가
        TeamMember teamMember = TeamMember.createMember(teamJoin.getTeam(), teamJoin.getTargetMember());
        teamMemberRepository.save(teamMember);

        // 팀장에게 수락 알림
        Team team = teamJoin.getTeam();
        Member acceptedMember = teamJoin.getTargetMember();
        teamMemberRepository.findLeaderIdByTeamId(team.getId()).ifPresent(leaderId ->
                notificationService.createNotification(
                        leaderId,
                        NotificationType.TEAM_INVITATION_ACCEPTED,
                        Map.of(
                                "teamId", team.getId(),
                                "teamName", team.getName(),
                                "memberId", acceptedMember.getId(),
                                "memberName", acceptedMember.getHandle()
                        )
                )
        );

        return TeamJoinResponse.from(teamJoin);
    }

    @Transactional
    public TeamJoinResponse reject(Long teamJoinId, Long memberId) {
        TeamJoin teamJoin = findAndValidateForProcess(teamJoinId, memberId, true);
        teamJoin.reject();

        // 팀장에게 거절 알림
        Team team = teamJoin.getTeam();
        Member rejectedMember = teamJoin.getTargetMember();
        teamMemberRepository.findLeaderIdByTeamId(team.getId()).ifPresent(leaderId ->
                notificationService.createNotification(
                        leaderId,
                        NotificationType.TEAM_INVITATION_REJECTED,
                        Map.of(
                                "teamId", team.getId(),
                                "teamName", team.getName(),
                                "memberId", rejectedMember.getId(),
                                "memberName", rejectedMember.getHandle()
                        )
                )
        );

        return TeamJoinResponse.from(teamJoin);
    }

    @Transactional
    public TeamJoinResponse cancel(Long teamJoinId, Long memberId) {
        TeamJoin teamJoin = findAndValidateForProcess(teamJoinId, memberId, false);
        teamJoin.cancel();
        return TeamJoinResponse.from(teamJoin);
    }

    @Transactional
    public TeamJoinResponse apply(Long teamId, Long applicantId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        Member applicant = memberRepository.findById(applicantId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        if (teamMemberRepository.existsByTeamIdAndMemberId(teamId, applicantId)) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_ALREADY_MEMBER);
        }

        if (teamJoinRepository.existsByTeamIdAndRequesterIdAndTypeAndStatus(
                teamId, applicantId, TeamJoinType.APPLICATION, TeamJoinStatus.PENDING)) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_ALREADY_EXISTS);
        }

        TeamJoin teamJoin = TeamJoin.createApplication(team, applicant);
        TeamJoin saved = teamJoinRepository.save(teamJoin);

        // 팀장에게 알림
        teamMemberRepository.findLeaderIdByTeamId(teamId).ifPresent(leaderId ->
                notificationService.createNotification(
                        leaderId,
                        NotificationType.TEAM_APPLICATION,
                        Map.of(
                                "teamId", team.getId(),
                                "teamName", team.getName(),
                                "applicantId", applicant.getId(),
                                "applicantName", applicant.getHandle()
                        )
                )
        );

        return TeamJoinResponse.from(saved);
    }

    @Transactional
    public TeamJoinResponse acceptApplication(Long teamJoinId, Long leaderId) {
        TeamJoin teamJoin = teamJoinRepository.findById(teamJoinId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_JOIN_NOT_FOUND));

        if (teamJoin.getType() != TeamJoinType.APPLICATION) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_NOT_FOUND);
        }

        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRole(
                teamJoin.getTeam().getId(), leaderId, TeamRole.LEADER)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        if (!teamJoin.isPending()) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_ALREADY_PROCESSED);
        }

        if (teamJoin.isExpired()) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_EXPIRED);
        }

        teamJoin.accept();

        TeamMember teamMember = TeamMember.createMember(teamJoin.getTeam(), teamJoin.getRequester());
        teamMemberRepository.save(teamMember);

        // 신청자에게 승인 알림
        Team team = teamJoin.getTeam();
        notificationService.createNotification(
                teamJoin.getRequester().getId(),
                NotificationType.TEAM_APPLICATION_ACCEPTED,
                Map.of("teamId", team.getId(), "teamName", team.getName())
        );

        return TeamJoinResponse.from(teamJoin);
    }

    @Transactional
    public TeamJoinResponse rejectApplication(Long teamJoinId, Long leaderId) {
        TeamJoin teamJoin = teamJoinRepository.findById(teamJoinId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_JOIN_NOT_FOUND));

        if (teamJoin.getType() != TeamJoinType.APPLICATION) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_NOT_FOUND);
        }

        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRole(
                teamJoin.getTeam().getId(), leaderId, TeamRole.LEADER)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        if (!teamJoin.isPending()) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_ALREADY_PROCESSED);
        }

        if (teamJoin.isExpired()) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_EXPIRED);
        }

        teamJoin.reject();

        // 신청자에게 거절 알림
        Team team = teamJoin.getTeam();
        notificationService.createNotification(
                teamJoin.getRequester().getId(),
                NotificationType.TEAM_APPLICATION_REJECTED,
                Map.of("teamId", team.getId(), "teamName", team.getName())
        );

        return TeamJoinResponse.from(teamJoin);
    }

    @Transactional(readOnly = true)
    public List<TeamJoinResponse> getApplicationList(Long teamId, Long leaderId) {
        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, leaderId, TeamRole.LEADER)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }

        return teamJoinRepository.findByTeamIdAndTypeAndStatusAndExpiresAtAfter(
                        teamId, TeamJoinType.APPLICATION, TeamJoinStatus.PENDING, LocalDateTime.now())
                .stream()
                .map(TeamJoinResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamJoinResponse> getReceivedList(Long memberId) {
        return teamJoinRepository.findByTargetMemberIdAndStatusAndExpiresAtAfter(
                        memberId, TeamJoinStatus.PENDING, LocalDateTime.now())
                .stream()
                .map(TeamJoinResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamJoinResponse> getSentList(Long memberId) {
        return teamJoinRepository.findByRequesterIdAndStatusAndExpiresAtAfter(
                        memberId, TeamJoinStatus.PENDING, LocalDateTime.now())
                .stream()
                .map(TeamJoinResponse::from)
                .toList();
    }

    private TeamJoin findAndValidateForProcess(Long teamJoinId, Long memberId, boolean isTargetAction) {
        TeamJoin teamJoin = teamJoinRepository.findById(teamJoinId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_JOIN_NOT_FOUND));

        // 권한 검증: isTargetAction이면 targetMember, 아니면 requester
        Long expectedMemberId = isTargetAction
                ? teamJoin.getTargetMember().getId()
                : teamJoin.getRequester().getId();

        if (!memberId.equals(expectedMemberId)) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_NO_PERMISSION);
        }

        if (!teamJoin.isPending()) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_ALREADY_PROCESSED);
        }

        if (teamJoin.isExpired()) {
            throw new CustomException(CustomResponseStatus.TEAM_JOIN_EXPIRED);
        }

        return teamJoin;
    }
}