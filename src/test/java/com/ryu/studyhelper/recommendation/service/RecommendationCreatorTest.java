package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.service.ProblemSyncService;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import com.ryu.studyhelper.team.repository.SquadIncludeTagRepository;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private SquadIncludeTagRepository squadIncludeTagRepository;

    @Mock
    private SolvedAcClient solvedAcClient;

    @Mock
    private ProblemSyncService problemSyncService;

    @Mock
    private RecommendationSaver recommendationSaver;

    @InjectMocks
    private RecommendationCreator recommendationCreator;

    private static final Long TEAM_ID = 1L;
    private static final Long SQUAD_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2025, 1, 15);

    @Nested
    @DisplayName("추천 생성 성공")
    class CreateSuccess {

        @Test
        @DisplayName("MANUAL 타입으로 추천을 생성한다")
        void createManualRecommendation() {
            // given
            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID);
            List<Problem> problems = List.of(createProblem(1000L), createProblem(1001L));
            Member member = createMember(100L);
            Recommendation pending = createPendingRecommendation();

            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of("handle1"));
            when(recommendationSaver.createOrResetPending(any(), any(), any())).thenReturn(pending);
            when(squadIncludeTagRepository.findTagKeysBySquadId(SQUAD_ID)).thenReturn(List.of());
            when(solvedAcClient.recommendUnsolvedProblems(anyList(), anyInt(), anyInt(), anyInt(), anyList()))
                    .thenReturn(List.of(mock(ProblemInfo.class)));
            when(problemSyncService.syncProblems(anyList())).thenReturn(problems);
            when(teamMemberRepository.findMembersByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of(member));

            // when
            Optional<Recommendation> result = recommendationCreator.createForSquad(squad, RecommendationType.MANUAL, TODAY);

            // then
            assertThat(result).isPresent();
            verify(recommendationSaver).saveSuccess(eq(pending), eq(problems), eq(List.of(member)), eq(squad));
        }

        @Test
        @DisplayName("SCHEDULED 타입으로 process를 실행한다")
        void processScheduledRecommendation() {
            // given
            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID);
            List<Problem> problems = List.of(createProblem(1000L));
            Member member = createMember(100L);
            Recommendation pending = createPendingRecommendation();

            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of("handle1"));
            when(squadIncludeTagRepository.findTagKeysBySquadId(SQUAD_ID)).thenReturn(List.of());
            when(solvedAcClient.recommendUnsolvedProblems(anyList(), anyInt(), anyInt(), anyInt(), anyList()))
                    .thenReturn(List.of(mock(ProblemInfo.class)));
            when(problemSyncService.syncProblems(anyList())).thenReturn(problems);
            when(teamMemberRepository.findMembersByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of(member));

            // when
            recommendationCreator.process(pending, squad);

            // then
            verify(recommendationSaver).saveSuccess(eq(pending), eq(problems), eq(List.of(member)), eq(squad));
        }

        @Test
        @DisplayName("스쿼드 멤버 수만큼 saveSuccess에 전달된다")
        void passesAllMembersToSaveSuccess() {
            // given
            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID);
            List<Problem> problems = List.of(createProblem(1000L));
            List<Member> members = List.of(createMember(100L), createMember(101L), createMember(102L));
            Recommendation pending = createPendingRecommendation();

            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of("handle1"));
            when(squadIncludeTagRepository.findTagKeysBySquadId(SQUAD_ID)).thenReturn(List.of());
            when(solvedAcClient.recommendUnsolvedProblems(anyList(), anyInt(), anyInt(), anyInt(), anyList()))
                    .thenReturn(List.of(mock(ProblemInfo.class)));
            when(problemSyncService.syncProblems(anyList())).thenReturn(problems);
            when(teamMemberRepository.findMembersByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(members);

            // when
            recommendationCreator.process(pending, squad);

            // then
            verify(recommendationSaver).saveSuccess(eq(pending), any(), eq(members), eq(squad));
        }
    }

    @Nested
    @DisplayName("추천 생성 스킵")
    class CreateSkip {

        @Test
        @DisplayName("인증된 핸들이 없으면 Optional.empty()를 반환한다")
        void noVerifiedHandle_returnsEmpty() {
            // given
            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID);

            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of());

            // when
            Optional<Recommendation> result = recommendationCreator.createForSquad(squad, RecommendationType.MANUAL, TODAY);

            // then
            assertThat(result).isEmpty();
            verify(recommendationSaver, never()).createOrResetPending(any(), any(), any());
            verify(solvedAcClient, never()).recommendUnsolvedProblems(any(), anyInt(), anyInt(), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("API 실패 처리")
    class ApiFailure {

        @Test
        @DisplayName("API 실패 시 saveFailed를 호출하고 예외를 전파한다")
        void apiFailure_callsSaveFailedAndRethrows() {
            // given
            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID);
            Recommendation pending = createPendingRecommendation();
            RuntimeException apiException = new RuntimeException("API 오류");

            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of("handle1"));
            when(squadIncludeTagRepository.findTagKeysBySquadId(SQUAD_ID)).thenReturn(List.of());
            when(solvedAcClient.recommendUnsolvedProblems(anyList(), anyInt(), anyInt(), anyInt(), anyList()))
                    .thenThrow(apiException);

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> recommendationCreator.process(pending, squad))
                    .isSameAs(apiException);

            verify(recommendationSaver).saveFailed(pending);
        }
    }

    @Nested
    @DisplayName("중복 추천 방지")
    class DuplicatePrevention {

        @Test
        @DisplayName("createOrResetPending에서 중복 예외가 발생하면 외부 API 호출 없이 전파한다")
        void duplicatePending_throwsAndSkipsApiCall() {
            // given
            Squad squad = createSquadWithId(SQUAD_ID, TEAM_ID);
            when(teamMemberRepository.findHandlesByTeamIdAndSquadId(TEAM_ID, SQUAD_ID))
                    .thenReturn(List.of("handle1"));
            when(recommendationSaver.createOrResetPending(any(), any(), any()))
                    .thenThrow(new CustomException(CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY));

            // when & then
            assertThatThrownBy(() -> recommendationCreator.createForSquad(squad, RecommendationType.MANUAL, TODAY))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.RECOMMENDATION_ALREADY_EXISTS_TODAY);

            verify(solvedAcClient, never()).recommendUnsolvedProblems(any(), anyInt(), anyInt(), anyInt(), any());
            verify(recommendationSaver, never()).saveSuccess(any(), any(), any(), any());
        }
    }

    // === Helper Methods ===

    private Squad createSquadWithId(Long squadId, Long teamId) {
        Team team = Team.create("테스트팀", "설명", false);
        setFieldValue(team, "id", teamId);

        Squad squad = Squad.createDefault(team);
        setFieldValue(squad, "id", squadId);
        return squad;
    }

    private Recommendation createPendingRecommendation() {
        return Recommendation.createPending(TEAM_ID, SQUAD_ID, RecommendationType.SCHEDULED, TODAY);
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
