package com.ryu.studyhelper.member;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.jwt.util.JwtUtil;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.infrastructure.mail.dto.MailHtmlSendDto;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.problem.ProblemRepository;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.solvedac.dto.SolvedAcUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private MemberSolvedProblemRepository memberSolvedProblemRepository;

    @Mock
    private SolvedAcService solvedacService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MailSendService mailSendService;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .handle("testHandle")
                .isVerified(true)
                .role(Role.ROLE_USER)
                .provider("google")
                .providerId("12345")
                .build();
    }

    @Nested
    @DisplayName("getById() - ID로 회원 조회")
    class GetByIdTests {

        @Test
        @DisplayName("성공: 존재하는 회원을 조회한다")
        void getById_Success() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));

            // when
            Member result = memberService.getById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            verify(memberRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 ID")
        void getById_NotFound() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.getById(999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("getAllByHandle() - 핸들로 회원 검색 (변경된 메서드)")
    class GetAllByHandleTests {

        @Test
        @DisplayName("성공: 핸들로 회원을 검색한다 - 단일 결과")
        void getAllByHandle_Success_SingleResult() {
            // given
            given(memberRepository.findAllByHandle("testHandle"))
                    .willReturn(Collections.singletonList(testMember));

            // when
            List<Member> results = memberService.getAllByHandle("testHandle");

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getHandle()).isEqualTo("testHandle");
            verify(memberRepository).findAllByHandle("testHandle");
        }

        @Test
        @DisplayName("성공: 핸들로 회원을 검색한다 - 다중 결과 (중복 핸들 허용)")
        void getAllByHandle_Success_MultipleResults() {
            // given
            Member member2 = Member.builder()
                    .id(2L)
                    .email("test2@example.com")
                    .handle("testHandle")
                    .isVerified(false)
                    .role(Role.ROLE_USER)
                    .provider("kakao")
                    .providerId("67890")
                    .build();

            given(memberRepository.findAllByHandle("testHandle"))
                    .willReturn(Arrays.asList(testMember, member2));

            // when
            List<Member> results = memberService.getAllByHandle("testHandle");

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getId()).isEqualTo(1L);
            assertThat(results.get(1).getId()).isEqualTo(2L);
            assertThat(results).allMatch(m -> m.getHandle().equals("testHandle"));
            verify(memberRepository).findAllByHandle("testHandle");
        }

        @Test
        @DisplayName("실패: 검색 결과가 없을 때 예외 발생 (변경된 동작)")
        void getAllByHandle_ThrowsException_WhenEmpty() {
            // given
            given(memberRepository.findAllByHandle("nonexistentHandle"))
                    .willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> memberService.getAllByHandle("nonexistentHandle"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

            verify(memberRepository).findAllByHandle("nonexistentHandle");
        }
    }

    @Nested
    @DisplayName("getVerifiedHandles() - 인증된 핸들 목록 조회")
    class GetVerifiedHandlesTests {

        @Test
        @DisplayName("성공: 인증된 핸들 목록을 조회한다")
        void getVerifiedHandles_Success() {
            // given
            List<String> expectedHandles = Arrays.asList("handle1", "handle2", "handle3");
            given(memberRepository.findAllVerifiedHandles()).willReturn(expectedHandles);

            // when
            List<String> results = memberService.getVerifiedHandles();

            // then
            assertThat(results).hasSize(3);
            assertThat(results).containsExactly("handle1", "handle2", "handle3");
            verify(memberRepository).findAllVerifiedHandles();
        }

        @Test
        @DisplayName("성공: 인증된 핸들이 없을 때 빈 리스트 반환")
        void getVerifiedHandles_EmptyList() {
            // given
            given(memberRepository.findAllVerifiedHandles()).willReturn(Collections.emptyList());

            // when
            List<String> results = memberService.getVerifiedHandles();

            // then
            assertThat(results).isEmpty();
            verify(memberRepository).findAllVerifiedHandles();
        }
    }

    @Nested
    @DisplayName("verifySolvedAcHandle() - 백준 핸들 인증")
    class VerifySolvedAcHandleTests {

        @Test
        @DisplayName("성공: solved.ac에서 핸들을 검증하고 저장한다")
        void verifySolvedAcHandle_Success() {
            // given
            SolvedAcUserResponse solvedAcUser = new SolvedAcUserResponse();
            given(solvedacService.getUserInfo("newHandle")).willReturn(solvedAcUser);
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));

            // when
            Member result = memberService.verifySolvedAcHandle(1L, "newHandle");

            // then
            assertThat(result.getHandle()).isEqualTo("newHandle");
            verify(solvedacService).getUserInfo("newHandle");
            verify(memberRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: solved.ac에 존재하지 않는 핸들")
        void verifySolvedAcHandle_HandleNotFound() {
            // given
            given(solvedacService.getUserInfo("invalidHandle"))
                    .willThrow(new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> memberService.verifySolvedAcHandle(1L, "invalidHandle"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND);

            verify(solvedacService).getUserInfo("invalidHandle");
            verify(memberRepository, never()).findById(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 ID")
        void verifySolvedAcHandle_MemberNotFound() {
            // given
            SolvedAcUserResponse solvedAcUser = new SolvedAcUserResponse();
            given(solvedacService.getUserInfo("validHandle")).willReturn(solvedAcUser);
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.verifySolvedAcHandle(999L, "validHandle"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

            verify(solvedacService).getUserInfo("validHandle");
            verify(memberRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("isEmailAvailable() - 이메일 중복 검사")
    class IsEmailAvailableTests {

        @Test
        @DisplayName("성공: 사용 가능한 이메일")
        void isEmailAvailable_Available() {
            // given
            given(memberRepository.findByEmail("new@example.com")).willReturn(Optional.empty());

            // when
            boolean result = memberService.isEmailAvailable("new@example.com");

            // then
            assertThat(result).isTrue();
            verify(memberRepository).findByEmail("new@example.com");
        }

        @Test
        @DisplayName("성공: 이미 사용 중인 이메일")
        void isEmailAvailable_AlreadyExists() {
            // given
            given(memberRepository.findByEmail("existing@example.com"))
                    .willReturn(Optional.of(testMember));

            // when
            boolean result = memberService.isEmailAvailable("existing@example.com");

            // then
            assertThat(result).isFalse();
            verify(memberRepository).findByEmail("existing@example.com");
        }
    }

    @Nested
    @DisplayName("sendEmailVerification() - 이메일 변경 인증 메일 발송")
    class SendEmailVerificationTests {

        @Test
        @DisplayName("성공: 이메일 변경 인증 메일을 발송한다")
        void sendEmailVerification_Success() {
            // given
            given(memberRepository.findByEmail("newemail@example.com")).willReturn(Optional.empty());
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(jwtUtil.createEmailVerificationToken(1L, "newemail@example.com"))
                    .willReturn("verification-token");

            // when
            memberService.sendEmailVerification(1L, "newemail@example.com");

            // then
            ArgumentCaptor<MailHtmlSendDto> mailCaptor = ArgumentCaptor.forClass(MailHtmlSendDto.class);
            verify(mailSendService).sendHtmlEmail(mailCaptor.capture());

            MailHtmlSendDto capturedMail = mailCaptor.getValue();
            assertThat(capturedMail.getEmailAddr()).isEqualTo("newemail@example.com");
            assertThat(capturedMail.getSubject()).contains("이메일 변경 인증");
            assertThat(capturedMail.getButtonUrl()).contains("verification-token");
            assertThat(capturedMail.getTemplateName()).isEqualTo("email-change-template");

            verify(memberRepository).findByEmail("newemail@example.com");
            verify(memberRepository).findById(1L);
            verify(jwtUtil).createEmailVerificationToken(1L, "newemail@example.com");
        }

        @Test
        @DisplayName("실패: 이미 사용 중인 이메일")
        void sendEmailVerification_EmailAlreadyExists() {
            // given
            given(memberRepository.findByEmail("existing@example.com"))
                    .willReturn(Optional.of(testMember));

            // when & then
            assertThatThrownBy(() -> memberService.sendEmailVerification(1L, "existing@example.com"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.EMAIL_ALREADY_EXISTS);

            verify(memberRepository).findByEmail("existing@example.com");
            verify(mailSendService, never()).sendHtmlEmail(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void sendEmailVerification_MemberNotFound() {
            // given
            given(memberRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.sendEmailVerification(999L, "new@example.com"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);

            verify(mailSendService, never()).sendHtmlEmail(any());
        }
    }

    @Nested
    @DisplayName("verifyAndChangeEmail() - 이메일 변경 인증 완료")
    class VerifyAndChangeEmailTests {

        @Test
        @DisplayName("성공: 토큰을 검증하고 이메일을 변경한다")
        void verifyAndChangeEmail_Success() {
            // given
            String token = "valid-token";
            given(jwtUtil.getTokenType(token)).willReturn(JwtUtil.TOKEN_TYPE_EMAIL_VERIFICATION);
            given(jwtUtil.getIdFromToken(token)).willReturn(1L);
            given(jwtUtil.getEmailFromToken(token)).willReturn("newemail@example.com");
            given(memberRepository.findByEmail("newemail@example.com")).willReturn(Optional.empty());
            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));

            // when
            Member result = memberService.verifyAndChangeEmail(token);

            // then
            assertThat(result.getEmail()).isEqualTo("newemail@example.com");
            verify(jwtUtil).validateTokenOrThrow(token);
            verify(jwtUtil).getTokenType(token);
            verify(jwtUtil).getIdFromToken(token);
            verify(jwtUtil).getEmailFromToken(token);
            verify(memberRepository).findByEmail("newemail@example.com");
            verify(memberRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 잘못된 토큰 타입")
        void verifyAndChangeEmail_InvalidTokenType() {
            // given
            String token = "wrong-type-token";
            given(jwtUtil.getTokenType(token)).willReturn("ACCESS");

            // when & then
            assertThatThrownBy(() -> memberService.verifyAndChangeEmail(token))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.INVALID_EMAIL_VERIFICATION_TOKEN);

            verify(jwtUtil).validateTokenOrThrow(token);
            verify(jwtUtil).getTokenType(token);
            verify(memberRepository, never()).findById(any());
        }

        @Test
        @DisplayName("실패: 인증 진행 중 이메일이 다른 사용자에게 할당됨")
        void verifyAndChangeEmail_EmailTakenDuringVerification() {
            // given
            String token = "valid-token";
            Member otherMember = Member.builder().id(2L).email("newemail@example.com").build();
            given(jwtUtil.getTokenType(token)).willReturn(JwtUtil.TOKEN_TYPE_EMAIL_VERIFICATION);
            given(jwtUtil.getIdFromToken(token)).willReturn(1L);
            given(jwtUtil.getEmailFromToken(token)).willReturn("newemail@example.com");
            given(memberRepository.findByEmail("newemail@example.com"))
                    .willReturn(Optional.of(otherMember));

            // when & then
            assertThatThrownBy(() -> memberService.verifyAndChangeEmail(token))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.EMAIL_ALREADY_EXISTS);

            verify(memberRepository).findByEmail("newemail@example.com");
            verify(memberRepository, never()).findById(any());
        }

        @Test
        @DisplayName("실패: 만료된 토큰")
        void verifyAndChangeEmail_ExpiredToken() {
            // given
            String token = "expired-token";
            doThrow(new CustomException(CustomResponseStatus.EXPIRED_JWT))
                    .when(jwtUtil).validateTokenOrThrow(token);

            // when & then
            assertThatThrownBy(() -> memberService.verifyAndChangeEmail(token))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("status", CustomResponseStatus.EXPIRED_JWT);

            verify(jwtUtil).validateTokenOrThrow(token);
            verify(memberRepository, never()).findById(any());
        }
    }
}