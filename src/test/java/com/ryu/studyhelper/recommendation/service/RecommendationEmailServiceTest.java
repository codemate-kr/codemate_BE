package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.infrastructure.mail.sender.MailMessage;
import com.ryu.studyhelper.infrastructure.mail.sender.MailSender;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.mailbuilder.RecommendationMailBuilder;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.team.domain.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationEmailService 테스트")
class RecommendationEmailServiceTest {

    @Mock
    private Clock clock;

    @Mock
    private MailSender mailSender;

    @Mock
    private RecommendationMailBuilder recommendationMailBuilder;

    @Mock
    private MemberRecommendationRepository memberRecommendationRepository;

    @InjectMocks
    private RecommendationEmailService recommendationEmailService;

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private void setupClock(String dateTime) {
        LocalDateTime ldt = LocalDateTime.parse(dateTime);
        Instant instant = ldt.atZone(ZONE_ID).toInstant();
        lenient().when(clock.instant()).thenReturn(instant);
        lenient().when(clock.getZone()).thenReturn(ZONE_ID);
    }

    @Nested
    @DisplayName("sendAll - 배치 이메일 발송")
    class SendAll {

        @Test
        @DisplayName("PENDING 상태의 추천들에 이메일을 발송한다")
        void sendsPendingEmails() {
            // given
            setupClock("2025-01-15T09:00:00");

            MemberRecommendation mr = createMemberRecommendation(1L, "user@test.com");
            when(memberRecommendationRepository.findPendingRecommendationsByCreatedAtBetween(
                    any(), any(), eq(EmailSendStatus.PENDING)))
                    .thenReturn(List.of(mr));
            when(recommendationMailBuilder.build(mr))
                    .thenReturn(new MailMessage("user@test.com", "제목", "<html>"));

            // when
            recommendationEmailService.sendAll();

            // then
            verify(mailSender).send(any(MailMessage.class));
            verify(memberRecommendationRepository).save(mr);
            assertThat(mr.getEmailSendStatus()).isEqualTo(EmailSendStatus.SENT);
        }

        @Test
        @DisplayName("PENDING 추천이 없으면 이메일을 발송하지 않는다")
        void noPending_sendsNothing() {
            // given
            setupClock("2025-01-15T09:00:00");

            when(memberRecommendationRepository.findPendingRecommendationsByCreatedAtBetween(
                    any(), any(), eq(EmailSendStatus.PENDING)))
                    .thenReturn(List.of());

            // when
            recommendationEmailService.sendAll();

            // then
            verify(mailSender, never()).send(any());
        }

        @Test
        @DisplayName("이메일 발송 실패 시 FAILED로 마킹하고 나머지는 계속 처리한다")
        void partialFailure_continuesProcessing() {
            // given
            setupClock("2025-01-15T09:00:00");

            MemberRecommendation mr1 = createMemberRecommendation(1L, "fail@test.com");
            MemberRecommendation mr2 = createMemberRecommendation(2L, "success@test.com");

            when(memberRecommendationRepository.findPendingRecommendationsByCreatedAtBetween(
                    any(), any(), eq(EmailSendStatus.PENDING)))
                    .thenReturn(List.of(mr1, mr2));

            MailMessage msg1 = new MailMessage("fail@test.com", "제목", "<html>");
            MailMessage msg2 = new MailMessage("success@test.com", "제목", "<html>");
            when(recommendationMailBuilder.build(mr1)).thenReturn(msg1);
            when(recommendationMailBuilder.build(mr2)).thenReturn(msg2);

            doThrow(new RuntimeException("SMTP 오류")).when(mailSender).send(msg1);
            doNothing().when(mailSender).send(msg2);

            // when
            recommendationEmailService.sendAll();

            // then
            assertThat(mr1.getEmailSendStatus()).isEqualTo(EmailSendStatus.FAILED);
            assertThat(mr2.getEmailSendStatus()).isEqualTo(EmailSendStatus.SENT);
            verify(memberRecommendationRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("send - 수동 추천 이메일 발송")
    class Send {

        @Test
        @DisplayName("전달된 MemberRecommendation 목록에 이메일을 발송한다")
        void sendsToAllMembers() {
            // given
            MemberRecommendation mr1 = createMemberRecommendation(1L, "a@test.com");
            MemberRecommendation mr2 = createMemberRecommendation(2L, "b@test.com");

            when(recommendationMailBuilder.build(any()))
                    .thenReturn(new MailMessage("to", "제목", "<html>"));

            // when
            recommendationEmailService.send(List.of(mr1, mr2));

            // then
            verify(mailSender, times(2)).send(any(MailMessage.class));
        }
    }

    @Nested
    @DisplayName("이메일 없는 회원 처리")
    class NoEmail {

        @Test
        @DisplayName("이메일이 null이면 FAILED 처리한다")
        void nullEmail_marksFailed() {
            // given
            MemberRecommendation mr = createMemberRecommendation(1L, null);

            // when
            recommendationEmailService.send(List.of(mr));

            // then
            assertThat(mr.getEmailSendStatus()).isEqualTo(EmailSendStatus.FAILED);
            verify(mailSender, never()).send(any());
        }

        @Test
        @DisplayName("이메일이 빈 문자열이면 FAILED 처리한다")
        void blankEmail_marksFailed() {
            // given
            MemberRecommendation mr = createMemberRecommendation(1L, "  ");

            // when
            recommendationEmailService.send(List.of(mr));

            // then
            assertThat(mr.getEmailSendStatus()).isEqualTo(EmailSendStatus.FAILED);
            verify(mailSender, never()).send(any());
        }
    }

    // === Helper Methods ===

    private MemberRecommendation createMemberRecommendation(Long id, String email) {
        Member member = Member.builder()
                .email(email)
                .provider("google")
                .providerId("provider-" + id)
                .isVerified(false)
                .build();
        setFieldValue(member, "id", id);

        Team team = Team.create("테스트팀", "설명", false);
        setFieldValue(team, "id", 1L);

        Recommendation recommendation = Recommendation.createScheduledRecommendation(1L);
        setFieldValue(recommendation, "createdAt", LocalDateTime.now(), true);

        MemberRecommendation mr = MemberRecommendation.create(member, recommendation, team);
        setFieldValue(mr, "id", id);
        return mr;
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        setFieldValue(target, fieldName, value, false);
    }

    private void setFieldValue(Object target, String fieldName, Object value, boolean superClass) {
        try {
            Class<?> clazz = superClass ? target.getClass().getSuperclass() : target.getClass();
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(fieldName + " 설정 실패", e);
        }
    }
}