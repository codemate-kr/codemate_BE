package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.problem.domain.Tag;
import com.ryu.studyhelper.problem.repository.TagRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.SquadIncludeTag;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamMember;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.team.dto.request.CreateSquadRequest;
import com.ryu.studyhelper.team.dto.request.SquadRecommendationSettingsRequest;
import com.ryu.studyhelper.team.dto.request.UpdateMemberSquadRequest;
import com.ryu.studyhelper.team.dto.request.UpdateSquadRequest;
import com.ryu.studyhelper.team.dto.response.SquadRecommendationSettingsResponse;
import com.ryu.studyhelper.team.dto.response.SquadResponse;
import com.ryu.studyhelper.team.dto.response.SquadSummaryResponse;
import com.ryu.studyhelper.team.repository.SquadIncludeTagRepository;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SquadService {

    private static final int MAX_SQUAD_COUNT = 5;

    private final TeamRepository teamRepository;
    private final SquadRepository squadRepository;
    private final SquadIncludeTagRepository squadIncludeTagRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TagRepository tagRepository;
    // TODO(#172): 2차 배포 시 제거 - initDefaultSquad() 제거 시 함께 제거
    private final TeamIncludeTagRepository teamIncludeTagRepository;

    @Transactional
    public SquadResponse createSquad(Long teamId, CreateSquadRequest request, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));
        validateTeamLeaderAccess(teamId, memberId);

        if (squadRepository.countByTeamId(teamId) >= MAX_SQUAD_COUNT) {
            throw new CustomException(CustomResponseStatus.SQUAD_LIMIT_EXCEEDED);
        }

        Squad squad = Squad.create(team, request.name(), request.description());
        Squad saved = squadRepository.save(squad);
        return SquadResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SquadSummaryResponse> getSquads(Long teamId, Long memberId) {
        validateTeamMemberAccess(teamId, memberId);

        List<Squad> squads = squadRepository.findByTeamIdOrderByIdAsc(teamId);

        Map<Long, Integer> memberCountBySquadId = new HashMap<>();
        for (Squad squad : squads) {
            memberCountBySquadId.put(
                    squad.getId(),
                    teamMemberRepository.countByTeamIdAndSquadId(teamId, squad.getId())
            );
        }

        return squads.stream()
                .map(squad -> SquadSummaryResponse.from(
                        squad,
                        memberCountBySquadId.getOrDefault(squad.getId(), 0),
                        squadIncludeTagRepository.findTagKeysBySquadId(squad.getId()),
                        null
                ))
                .toList();
    }

    @Transactional
    public SquadResponse updateSquad(Long teamId, Long squadId, UpdateSquadRequest request, Long memberId) {
        validateTeamLeaderAccess(teamId, memberId);

        Squad squad = squadRepository.findByIdAndTeamId(squadId, teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        squad.updateBasicInfo(request.name(), request.description());
        squadRepository.save(squad);
        return SquadResponse.from(squad);
    }

    @Transactional
    public void deleteSquad(Long teamId, Long squadId, Long memberId) {
        validateTeamLeaderAccess(teamId, memberId);

        Squad squad = squadRepository.findByIdAndTeamId(squadId, teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        if (squad.isDefault()) {
            throw new CustomException(CustomResponseStatus.DEFAULT_SQUAD_CANNOT_DELETE);
        }

        Squad defaultSquad = findDefaultSquad(teamId);

        // 삭제 대상 스쿼드 소속 멤버를 기본 스쿼드로 이동
        List<TeamMember> targetMembers = teamMemberRepository.findByTeamIdAndSquadId(teamId, squadId);
        for (TeamMember member : targetMembers) {
            member.updateSquadId(defaultSquad.getId());
        }
        teamMemberRepository.saveAll(targetMembers);

        squadIncludeTagRepository.deleteAllBySquadId(squadId);
        squadRepository.delete(squad);
    }

    @Transactional(readOnly = true)
    public SquadRecommendationSettingsResponse getRecommendationSettings(Long teamId, Long squadId, Long memberId) {
        validateTeamMemberAccess(teamId, memberId);

        Squad squad = squadRepository.findByIdAndTeamId(squadId, teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        List<String> includeTags = squadIncludeTagRepository.findTagKeysBySquadId(squadId);
        return SquadRecommendationSettingsResponse.from(squad, includeTags);
    }

    @Transactional
    public SquadRecommendationSettingsResponse updateRecommendationSettings(
            Long teamId,
            Long squadId,
            SquadRecommendationSettingsRequest request,
            Long memberId
    ) {
        validateTeamLeaderAccess(teamId, memberId);

        Squad squad = squadRepository.findByIdAndTeamId(squadId, teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        if (request.problemDifficultyPreset().isCustom()) {
            if (request.minProblemLevel() != null && request.maxProblemLevel() != null
                    && request.minProblemLevel() > request.maxProblemLevel()) {
                throw new CustomException(CustomResponseStatus.INVALID_PROBLEM_LEVEL_RANGE);
            }
        }

        squad.updateRecommendationDays(request.recommendationDays());
        squad.updateProblemDifficultySettings(
                request.problemDifficultyPreset(),
                request.minProblemLevel(),
                request.maxProblemLevel()
        );
        squad.updateProblemCount(request.problemCount());

        squadRepository.save(squad);

        List<String> includeTags = updateIncludeTags(squad, request.includeTags());
        return SquadRecommendationSettingsResponse.from(squad, includeTags);
    }

    @Transactional
    public SquadRecommendationSettingsResponse disableRecommendation(Long teamId, Long squadId, Long memberId) {
        validateTeamLeaderAccess(teamId, memberId);

        Squad squad = squadRepository.findByIdAndTeamId(squadId, teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        squad.updateRecommendationDays(List.of());
        squadRepository.save(squad);

        List<String> includeTags = squadIncludeTagRepository.findTagKeysBySquadId(squadId);
        return SquadRecommendationSettingsResponse.from(squad, includeTags);
    }

    @Transactional
    public void changeMemberSquad(Long teamId, Long memberId, UpdateMemberSquadRequest request, Long leaderId) {
        validateTeamLeaderAccess(teamId, leaderId);

        TeamMember target = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_MEMBER_NOT_FOUND));

        Squad squad = squadRepository.findByIdAndTeamId(request.squadId(), teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.SQUAD_NOT_FOUND));

        target.updateSquadId(squad.getId());
        teamMemberRepository.save(target);
    }

    // TODO(#172): 2차 배포 시 단순화 - 모든 팀에 기본 스쿼드 생성 완료 후 lazy 초기화 분기 제거,
    //             단순 조회(orElseThrow SQUAD_NOT_FOUND)로 교체
    @Transactional
    public Squad findDefaultSquad(Long teamId) {
        // 1차 확인 (락 없이 빠른 패스)
        Optional<Squad> existing = squadRepository.findFirstByTeamIdAndIsDefaultTrueOrderByIdAsc(teamId);
        if (existing.isPresent()) return existing.get();

        // 기본 스쿼드 없음 → 팀 행에 비관적 쓰기 락
        teamRepository.findByIdWithPessimisticLock(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        // 2차 확인 (락 획득 후 재확인, 다른 스레드가 먼저 생성했을 수 있음)
        return squadRepository.findFirstByTeamIdAndIsDefaultTrueOrderByIdAsc(teamId)
                .orElseGet(() -> initDefaultSquad(teamId));
    }

    // TODO(#172): 2차 배포 시 제거 - 1차 배포 기간에만 필요한 backward compat 초기화 메서드.
    //             TeamIncludeTagRepository 의존성, TeamMemberRepository.findByTeamIdAndSquadIdIsNull()도 함께 제거
    /** 기존 팀에 기본 스쿼드 없는 경우 자동 초기화 (1차 배포 backward compat) */
    private Squad initDefaultSquad(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        Squad defaultSquad = squadRepository.save(Squad.createDefault(team));

        // squad 미배정 팀원을 기본 스쿼드에 배정
        List<TeamMember> unassigned = teamMemberRepository.findByTeamIdAndSquadIdIsNull(teamId);
        unassigned.forEach(m -> m.updateSquadId(defaultSquad.getId()));
        if (!unassigned.isEmpty()) {
            teamMemberRepository.saveAll(unassigned);
        }

        // TeamIncludeTag → SquadIncludeTag 복사
        List<String> tagKeys = teamIncludeTagRepository.findTagKeysByTeamId(teamId);
        if (!tagKeys.isEmpty()) {
            List<Tag> tags = tagRepository.findByKeyIn(tagKeys);
            squadIncludeTagRepository.saveAll(
                    tags.stream().map(tag -> SquadIncludeTag.create(defaultSquad, tag)).toList()
            );
        }

        log.info("팀 {} 기본 스쿼드 자동 초기화 완료 (멤버: {}명, 태그: {}개)",
                teamId, unassigned.size(), tagKeys.size());
        return defaultSquad;
    }

    private void validateTeamLeaderAccess(Long teamId, Long memberId) {
        boolean isLeader = teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER);
        if (!isLeader) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }

    private void validateTeamMemberAccess(Long teamId, Long memberId) {
        boolean isMember = teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId);
        if (!isMember) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }

    private List<String> updateIncludeTags(Squad squad, List<String> tagKeys) {
        List<String> keys = tagKeys != null ? tagKeys : List.of();

        squadIncludeTagRepository.deleteAllBySquadId(squad.getId());

        if (keys.isEmpty()) {
            return List.of();
        }

        List<Tag> validTags = tagRepository.findByKeyIn(keys);

        if (validTags.size() != keys.size()) {
            Set<String> validKeys = validTags.stream()
                    .map(Tag::getKey)
                    .collect(Collectors.toSet());
            List<String> invalidKeys = keys.stream()
                    .filter(k -> !validKeys.contains(k))
                    .toList();
            log.warn("스쿼드 {} 태그 설정 시 무효한 태그 키 무시됨: {}", squad.getId(), invalidKeys);
        }

        List<SquadIncludeTag> includeTags = validTags.stream()
                .map(tag -> SquadIncludeTag.create(squad, tag))
                .toList();

        squadIncludeTagRepository.saveAll(includeTags);

        return validTags.stream()
                .map(Tag::getKey)
                .toList();
    }
}
