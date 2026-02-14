package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.service.ProblemService;
import com.ryu.studyhelper.problem.service.ProblemSyncService;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.TeamIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationCreator 테스트")
class RecommendationCreatorTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamIncludeTagRepository teamIncludeTagRepository;

    @Mock
    private ProblemService problemService;

    @Mock
    private ProblemSyncService problemSyncService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationProblemRepository recommendationProblemRepository;

    @Mock
    private MemberRecommendationRepository memberRecommendationRepository;

    @InjectMocks
    private RecommendationCreator recommendationCreator;

    private static final Long TEAM_ID = 1L;

    @Nested
    @DisplayName("추천 생성 성공")
    class CreateSuccess {

        @Test
        @DisplayName("MANUAL 타입으로 추천을 생성한다")
        void createManualRecommendation() {
            // given
            Team team = createTeamWithId(TEAM_ID);
            List<Problem> problems = List.of(createProblem(1000L), createProblem(1001L));
            Member member = createMember(100L);

            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID)).thenReturn(List.of("handle1"));
            when(teamIncludeTagRepository.findTagKeysByTeamId(TEAM_ID)).thenReturn(List.of());
            when(problemService.recommend(anyList(), anyInt(), anyInt(), anyInt(), anyList()))
                    .thenReturn(List.of(mock(ProblemInfo.class)));
            when(problemSyncService.syncProblems(anyList())).thenReturn(problems);
            when(recommendationRepository.save(any(Recommendation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findMembersByTeamId(TEAM_ID)).thenReturn(List.of(member));

            // when
            Recommendation result = recommendationCreator.create(team, RecommendationType.MANUAL);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(RecommendationType.MANUAL);
            assertThat(result.getProblems()).hasSize(2);
            verify(recommendationProblemRepository, times(2)).save(any());
            verify(memberRecommendationRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("SCHEDULED 타입으로 추천을 생성한다")
        void createScheduledRecommendation() {
            // given
            Team team = createTeamWithId(TEAM_ID);
            List<Problem> problems = List.of(createProblem(1000L));
            Member member = createMember(100L);

            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID)).thenReturn(List.of("handle1"));
            when(teamIncludeTagRepository.findTagKeysByTeamId(TEAM_ID)).thenReturn(List.of());
            when(problemService.recommend(anyList(), anyInt(), anyInt(), anyInt(), anyList()))
                    .thenReturn(List.of(mock(ProblemInfo.class)));
            when(problemSyncService.syncProblems(anyList())).thenReturn(problems);
            when(recommendationRepository.save(any(Recommendation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findMembersByTeamId(TEAM_ID)).thenReturn(List.of(member));

            // when
            Recommendation result = recommendationCreator.create(team, RecommendationType.SCHEDULED);

            // then
            assertThat(result.getType()).isEqualTo(RecommendationType.SCHEDULED);
        }

        @Test
        @DisplayName("팀원 수만큼 MemberRecommendation을 생성한다")
        void createsMemberRecommendationsForAllMembers() {
            // given
            Team team = createTeamWithId(TEAM_ID);
            List<Problem> problems = List.of(createProblem(1000L));
            List<Member> members = List.of(createMember(100L), createMember(101L), createMember(102L));

            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID)).thenReturn(List.of("handle1"));
            when(teamIncludeTagRepository.findTagKeysByTeamId(TEAM_ID)).thenReturn(List.of());
            when(problemService.recommend(anyList(), anyInt(), anyInt(), anyInt(), anyList()))
                    .thenReturn(List.of(mock(ProblemInfo.class)));
            when(problemSyncService.syncProblems(anyList())).thenReturn(problems);
            when(recommendationRepository.save(any(Recommendation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findMembersByTeamId(TEAM_ID)).thenReturn(members);

            // when
            recommendationCreator.create(team, RecommendationType.MANUAL);

            // then
            verify(memberRecommendationRepository, times(3)).save(any());
        }
    }

    @Nested
    @DisplayName("추천 생성 실패")
    class CreateFailure {

        @Test
        @DisplayName("인증된 핸들이 없으면 CustomException을 던진다")
        void noVerifiedHandle_throwsException() {
            // given
            Team team = createTeamWithId(TEAM_ID);

            when(teamMemberRepository.findHandlesByTeamId(TEAM_ID)).thenReturn(List.of());
            when(recommendationRepository.save(any(Recommendation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when & then
            assertThatThrownBy(() -> recommendationCreator.create(team, RecommendationType.MANUAL))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException customEx = (CustomException) ex;
                        assertThat(customEx.getStatus()).isEqualTo(CustomResponseStatus.NO_VERIFIED_HANDLE);
                    });
        }
    }

    // === Helper Methods ===

    private Team createTeamWithId(Long id) {
        Team team = Team.create("테스트팀", "설명", false);
        setFieldValue(team, "id", id);
        return team;
    }

    private Problem createProblem(Long id) {
        Problem problem = Problem.builder()
                .title("Problem " + id)
                .titleKo("문제 " + id)
                .level(10)
                .acceptedUserCount(100)
                .averageTries(2.5)
                .build();
        setFieldValue(problem, "id", id);
        return problem;
    }

    private Member createMember(Long id) {
        Member member = Member.builder()
                .email("test" + id + "@test.com")
                .provider("google")
                .providerId("provider-" + id)
                .isVerified(false)
                .build();
        setFieldValue(member, "id", id);
        return member;
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " 설정 실패", e);
        }
    }
}