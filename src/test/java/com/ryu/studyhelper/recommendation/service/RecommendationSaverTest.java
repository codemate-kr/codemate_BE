package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.RecommendationStatus;
import com.ryu.studyhelper.recommendation.domain.RecommendationType;
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationRepository;
import com.ryu.studyhelper.team.domain.Squad;
import com.ryu.studyhelper.team.domain.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RecommendationSaver 단위 테스트
 *
 * 검증 목표:
 * - saveSuccess() 호출 후 rec.getProblems()가 in-memory에서 동기화되는지
 * - REQUIRES_NEW 메서드가 반환하는 객체의 내부 상태 계약
 *
 * 재발 방지 배경:
 * saveSuccess()가 DB에 RecommendationProblem을 저장했지만 rec.problems 컬렉션에는
 * 추가하지 않아, 수동 추천 직후 메일 빌더가 빈 문제 목록으로 메일을 발송하는 버그가 발생했다.
 * (@Transactional REQUIRES_NEW로 커밋 후 반환된 rec는 detached 상태이므로 lazy load 불가)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationSaver 단위 테스트")
class RecommendationSaverTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationProblemRepository recommendationProblemRepository;

    @Mock
    private MemberRecommendationRepository memberRecommendationRepository;

    @InjectMocks
    private RecommendationSaver recommendationSaver;

    private static final Long TEAM_ID = 1L;
    private static final Long SQUAD_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2025, 1, 15);

    @Nested
    @DisplayName("saveSuccess - 성공 저장")
    class SaveSuccess {

        /**
         * 핵심 회귀 테스트.
         * saveSuccess() 후 반환된 MemberRecommendation들의 recommendation.getProblems()가
         * 비어있으면, 메일 빌더가 0개 문제로 메일을 발송하는 버그가 재발한다.
         */
        @Test
        @DisplayName("저장된 문제가 rec.getProblems()에 반영된다 (메일 빌더 in-memory 계약)")
        void savedProblems_areReflectedInRecProblems() {
            // given
            Recommendation rec = createPendingRecommendation();
            Problem p1 = createProblem(1000L);
            Problem p2 = createProblem(1001L);
            List<Problem> problems = List.of(p1, p2);

            Squad squad = createSquad();
            Member member = createMember(100L, "user@test.com");

            RecommendationProblem rp1 = RecommendationProblem.create(p1, rec);
            RecommendationProblem rp2 = RecommendationProblem.create(p2, rec);
            when(recommendationProblemRepository.save(any()))
                    .thenReturn(rp1, rp2);
            when(memberRecommendationRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(recommendationRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // when
            List<MemberRecommendation> result = recommendationSaver.saveSuccess(rec, problems, List.of(member), squad);

            // then: 반환된 MemberRecommendation의 recommendation.problems가 채워져 있어야 한다
            // (메일 빌더는 mr.getRecommendation().getProblems()로 문제 목록을 읽는다)
            assertThat(result).hasSize(1);
            Recommendation recommendation = result.get(0).getRecommendation();
            assertThat(recommendation.getProblems())
                    .as("수동 추천 메일 빌더가 읽는 rec.problems가 비어 있으면 0개 문제로 메일이 발송됨")
                    .hasSize(2);
        }

        @Test
        @DisplayName("저장된 문제 수와 rec.getProblems() 크기가 일치한다")
        void problemCount_matchesSavedCount() {
            // given
            Recommendation rec = createPendingRecommendation();
            List<Problem> problems = List.of(
                    createProblem(1000L), createProblem(1001L), createProblem(1002L)
            );
            Squad squad = createSquad();

            when(recommendationProblemRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(memberRecommendationRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(recommendationRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // when
            recommendationSaver.saveSuccess(rec, problems, List.of(createMember(1L, "a@b.com")), squad);

            // then
            assertThat(rec.getProblems()).hasSize(3);
        }

        @Test
        @DisplayName("성공 저장 후 rec 상태가 SUCCESS로 전이된다")
        void saveSuccess_transitionsToSuccess() {
            // given
            Recommendation rec = createPendingRecommendation();
            Squad squad = createSquad();
            Member member = createMember(100L, "user@test.com");

            when(recommendationProblemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberRecommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            recommendationSaver.saveSuccess(rec, List.of(createProblem(1000L)), List.of(member), squad);

            // then
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.SUCCESS);
        }

        @Test
        @DisplayName("멤버 수만큼 MemberRecommendation이 생성된다")
        void memberCount_matchesMemberRecommendationCount() {
            // given
            Recommendation rec = createPendingRecommendation();
            Squad squad = createSquad();
            List<Member> members = List.of(
                    createMember(1L, "a@test.com"),
                    createMember(2L, "b@test.com"),
                    createMember(3L, "c@test.com")
            );

            when(recommendationProblemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberRecommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<MemberRecommendation> result = recommendationSaver.saveSuccess(
                    rec, List.of(createProblem(1000L)), members, squad);

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("생성된 MemberRecommendation의 초기 이메일 상태는 PENDING이다")
        void memberRecommendation_initialEmailStatus_isPending() {
            // given
            Recommendation rec = createPendingRecommendation();
            Squad squad = createSquad();
            Member member = createMember(100L, "user@test.com");

            when(recommendationProblemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberRecommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<MemberRecommendation> result = recommendationSaver.saveSuccess(
                    rec, List.of(createProblem(1000L)), List.of(member), squad);

            // then
            assertThat(result.get(0).getEmailSendStatus()).isEqualTo(EmailSendStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("saveFailed - 실패 저장")
    class SaveFailed {

        @Test
        @DisplayName("PENDING rec를 FAILED로 전이한다")
        void saveFailed_transitionsToFailed() {
            // given
            Recommendation rec = createPendingRecommendation();
            when(recommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            recommendationSaver.saveFailed(rec);

            // then
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("tryPrepareForRetry - 재시도 선점")
    class TryPrepareForRetry {

        @Test
        @DisplayName("compareAndUpdateStatus가 1을 반환하면 true를 반환하고 in-memory 상태를 PENDING으로 동기화한다")
        void claimed_returnsTrueAndSyncsToPending() {
            // given
            Recommendation rec = createPendingRecommendation();
            rec.markAsFailed();
            when(recommendationRepository.compareAndUpdateStatus(any(), any(), any())).thenReturn(1);

            // when
            boolean result = recommendationSaver.tryPrepareForRetry(rec);

            // then
            assertThat(result).isTrue();
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.PENDING);
        }

        @Test
        @DisplayName("compareAndUpdateStatus가 0을 반환하면 false를 반환하고 상태를 변경하지 않는다")
        void notClaimed_returnsFalse() {
            // given
            Recommendation rec = createPendingRecommendation();
            rec.markAsFailed();
            when(recommendationRepository.compareAndUpdateStatus(any(), any(), any())).thenReturn(0);

            // when
            boolean result = recommendationSaver.tryPrepareForRetry(rec);

            // then
            assertThat(result).isFalse();
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.FAILED);
        }
    }

    // === Helper Methods ===

    private Recommendation createPendingRecommendation() {
        return Recommendation.createPending(TEAM_ID, SQUAD_ID, RecommendationType.MANUAL, TODAY);
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

    private Member createMember(Long id, String email) {
        Member member = Member.builder()
                .email(email)
                .provider("google")
                .providerId("provider-" + id)
                .isVerified(false)
                .build();
        setFieldValue(member, "id", id);
        return member;
    }

    private Squad createSquad() {
        Team team = Team.create("테스트팀", "설명", false);
        setFieldValue(team, "id", TEAM_ID);
        Squad squad = Squad.createDefault(team);
        setFieldValue(squad, "id", SQUAD_ID);
        return squad;
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
