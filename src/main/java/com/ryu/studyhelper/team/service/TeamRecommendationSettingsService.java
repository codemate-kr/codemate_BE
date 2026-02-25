package com.ryu.studyhelper.team.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.problem.domain.Tag;
import com.ryu.studyhelper.problem.repository.TagRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.SquadIncludeTag;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.domain.TeamIncludeTag;
import com.ryu.studyhelper.team.domain.TeamRole;
import com.ryu.studyhelper.team.dto.request.TeamRecommendationSettingsRequest;
import com.ryu.studyhelper.team.dto.response.TeamRecommendationSettingsResponse;
import com.ryu.studyhelper.team.repository.SquadIncludeTagRepository;
import com.ryu.studyhelper.team.repository.SquadRepository;
import com.ryu.studyhelper.team.repository.TeamIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @deprecated Dual Write 전환 완료 후 제거 예정.
 * 팀 수준 추천 설정은 스쿼드 수준으로 이전됩니다.
 * 대체: {@link SquadService}
 */
@Deprecated(forRemoval = true)
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TeamRecommendationSettingsService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamIncludeTagRepository teamIncludeTagRepository;
    private final SquadIncludeTagRepository squadIncludeTagRepository;
    private final SquadRepository squadRepository;
    private final TagRepository tagRepository;
    private final SquadService squadService;

    // TODO(#172): 2차 배포 시 클래스 전체 제거
    @Transactional(readOnly = true)
    public TeamRecommendationSettingsResponse getRecommendationSettings(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        List<String> includeTags = teamIncludeTagRepository.findTagKeysByTeamId(teamId);
        return TeamRecommendationSettingsResponse.from(team, includeTags);
    }

    // TODO(#172): 2차 배포 시 클래스 전체 제거
    public TeamRecommendationSettingsResponse updateRecommendationSettings(
            Long teamId,
            TeamRecommendationSettingsRequest request,
            Long memberId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateTeamLeaderAccess(teamId, memberId);

        if (request.problemDifficultyPreset().isCustom()) {
            if (request.minProblemLevel() != null && request.maxProblemLevel() != null
                    && request.minProblemLevel() > request.maxProblemLevel()) {
                throw new CustomException(CustomResponseStatus.INVALID_PROBLEM_LEVEL_RANGE);
            }
        }

        team.updateRecommendationDays(request.recommendationDays());
        team.updateProblemDifficultySettings(
                request.problemDifficultyPreset(),
                request.minProblemLevel(),
                request.maxProblemLevel()
        );
        team.updateProblemCount(request.problemCount());
        teamRepository.save(team);

        List<Tag> validTags = updateTeamIncludeTags(team, request.includeTags());

        // Dual Write: 기본 스쿼드도 동기화
        Squad defaultSquad = squadService.findDefaultSquad(teamId);
        defaultSquad.updateRecommendationDays(request.recommendationDays());
        defaultSquad.updateProblemDifficultySettings(
                request.problemDifficultyPreset(),
                request.minProblemLevel(),
                request.maxProblemLevel()
        );
        defaultSquad.updateProblemCount(request.problemCount());
        squadRepository.save(defaultSquad);
        syncSquadIncludeTags(defaultSquad, validTags);

        return TeamRecommendationSettingsResponse.from(team,
                validTags.stream().map(Tag::getKey).toList());
    }

    // TODO(#172): 2차 배포 시 클래스 전체 제거
    public TeamRecommendationSettingsResponse disableRecommendation(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

        validateTeamLeaderAccess(teamId, memberId);

        team.updateRecommendationDays(List.of());
        teamRepository.save(team);

        // Dual Write: 기본 스쿼드도 비활성화
        Squad defaultSquad = squadService.findDefaultSquad(teamId);
        defaultSquad.updateRecommendationDays(List.of());
        squadRepository.save(defaultSquad);

        return TeamRecommendationSettingsResponse.from(team);
    }

    /**
     * 팀 포함 태그 전체 교체.
     * 유효한 태그 엔티티를 반환하여 Dual Write 시 재사용할 수 있도록 함.
     */
    // TODO(#172): 2차 배포 시 클래스 전체 제거
    private List<Tag> updateTeamIncludeTags(Team team, List<String> tagKeys) {
        List<String> keys = tagKeys != null ? tagKeys : List.of();

        teamIncludeTagRepository.deleteAllByTeamId(team.getId());

        if (keys.isEmpty()) {
            return List.of();
        }

        List<Tag> validTags = tagRepository.findByKeyIn(keys);

        if (validTags.size() != keys.size()) {
            Set<String> validKeys = validTags.stream().map(Tag::getKey).collect(Collectors.toSet());
            List<String> invalidKeys = keys.stream().filter(k -> !validKeys.contains(k)).toList();
            log.warn("팀 {} 태그 설정 시 무효한 태그 키 무시됨: {}", team.getId(), invalidKeys);
        }

        teamIncludeTagRepository.saveAll(
                validTags.stream()
                        .map(tag -> TeamIncludeTag.builder().team(team).tag(tag).build())
                        .toList()
        );

        return validTags;
    }

    /**
     * 이미 검증된 Tag 엔티티를 받아 스쿼드 포함 태그를 교체.
     * updateTeamIncludeTags에서 로드한 Tag를 재사용하여 중복 쿼리 방지.
     */
    // TODO(#172): 2차 배포 시 클래스 전체 제거 - Dual Write 지원 메서드
    private void syncSquadIncludeTags(Squad squad, List<Tag> tags) {
        squadIncludeTagRepository.deleteAllBySquadId(squad.getId());

        if (tags.isEmpty()) {
            return;
        }

        squadIncludeTagRepository.saveAll(
                tags.stream().map(tag -> SquadIncludeTag.create(squad, tag)).toList()
        );
    }

    private void validateTeamLeaderAccess(Long teamId, Long memberId) {
        if (!teamMemberRepository.existsByTeamIdAndMemberIdAndRole(teamId, memberId, TeamRole.LEADER)) {
            throw new CustomException(CustomResponseStatus.TEAM_ACCESS_DENIED);
        }
    }
}
