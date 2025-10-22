package com.ryu.studyhelper.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.member.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("MemberController 단위 테스트")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    private Member testMember;
    private PrincipalDetails principalDetails;

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

        principalDetails = new PrincipalDetails(testMember);
    }

    @Nested
    @DisplayName("GET /api/member/me - 내 프로필 조회")
    class GetMyProfileTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 인증된 사용자의 프로필을 조회한다")
        void getMyProfile_Success() throws Exception {
            // given
            given(memberService.getById(1L)).willReturn(testMember);

            // when & then
            mockMvc.perform(get("/api/member/me")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.handle").value("testHandle"))
                    .andExpect(jsonPath("$.data.verified").value(true));

            verify(memberService).getById(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 존재하지 않는 사용자")
        void getMyProfile_NotFound() throws Exception {
            // given
            given(memberService.getById(1L))
                    .willThrow(new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/member/me")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/member/{id} - 멤버 공개 정보 조회")
    class GetMemberPublicInfoTests {

        @Test
        @DisplayName("성공: 특정 멤버의 공개 정보를 조회한다")
        void getMemberPublicInfo_Success() throws Exception {
            // given
            given(memberService.getById(1L)).willReturn(testMember);

            // when & then
            mockMvc.perform(get("/api/member/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.handle").value("testHandle"));

            verify(memberService).getById(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 멤버 ID")
        void getMemberPublicInfo_NotFound() throws Exception {
            // given
            given(memberService.getById(999L))
                    .willThrow(new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/member/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/member/search - 핸들로 멤버 검색")
    class SearchMemberByHandleTests {

        @Test
        @DisplayName("성공: 핸들로 멤버를 검색한다 - 단일 결과")
        void searchByHandle_Success_SingleResult() throws Exception {
            // given
            given(memberService.getAllByHandle("testHandle"))
                    .willReturn(Collections.singletonList(testMember));

            // when & then
            mockMvc.perform(get("/api/member/search")
                            .param("handle", "testHandle"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(1L))
                    .andExpect(jsonPath("$.data[0].handle").value("testHandle"))
                    .andExpect(jsonPath("$.data[0].verified").value(true))
                    .andExpect(jsonPath("$.data[0].email").value("test@example.com"));

            verify(memberService).getAllByHandle("testHandle");
        }

        @Test
        @DisplayName("성공: 핸들로 멤버를 검색한다 - 다중 결과 (중복 핸들)")
        void searchByHandle_Success_MultipleResults() throws Exception {
            // given
            Member member2 = Member.builder()
                    .id(2L)
                    .email("test2@example.com")
                    .handle("testHandle")
                    .isVerified(true)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("67890")
                    .build();

            given(memberService.getAllByHandle("testHandle"))
                    .willReturn(Arrays.asList(testMember, member2));

            // when & then
            mockMvc.perform(get("/api/member/search")
                            .param("handle", "testHandle"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1L))
                    .andExpect(jsonPath("$.data[1].id").value(2L));

            verify(memberService).getAllByHandle("testHandle");
        }

        @Test
        @DisplayName("실패: 검색 결과가 없을 때 예외 발생")
        void searchByHandle_NotFound() throws Exception {
            // given
            given(memberService.getAllByHandle("nonexistentHandle"))
                    .willThrow(new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/member/search")
                            .param("handle", "nonexistentHandle"))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(memberService).getAllByHandle("nonexistentHandle");
        }

        @Test
        @DisplayName("실패: handle 파라미터가 없을 때")
        void searchByHandle_MissingParameter() throws Exception {
            // when & then
            mockMvc.perform(get("/api/member/search"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/member/handles - 인증된 핸들 목록 조회")
    class GetVerifiedHandlesTests {

        @Test
        @DisplayName("성공: 인증된 핸들 목록을 조회한다")
        void getVerifiedHandles_Success() throws Exception {
            // given
            List<String> handles = Arrays.asList("handle1", "handle2", "handle3");
            given(memberService.getVerifiedHandles()).willReturn(handles);

            // when & then
            mockMvc.perform(get("/api/member/handles"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0]").value("handle1"))
                    .andExpect(jsonPath("$.data[1]").value("handle2"))
                    .andExpect(jsonPath("$.data[2]").value("handle3"));

            verify(memberService).getVerifiedHandles();
        }

        @Test
        @DisplayName("성공: 인증된 핸들이 없을 때 빈 배열 반환")
        void getVerifiedHandles_EmptyList() throws Exception {
            // given
            given(memberService.getVerifiedHandles()).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/member/handles"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(memberService).getVerifiedHandles();
        }
    }

    @Nested
    @DisplayName("POST /api/member/me/verify-solvedac - 백준 핸들 인증")
    class VerifySolvedAcTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 백준 핸들을 인증한다")
        void verifySolvedAc_Success() throws Exception {
            // given
            Member updatedMember = Member.builder()
                    .id(1L)
                    .email("test@example.com")
                    .handle("newHandle")
                    .isVerified(true)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("12345")
                    .build();

            VerifySolvedAcRequest request = new VerifySolvedAcRequest("newHandle");
            given(memberService.verifySolvedAcHandle(1L, "newHandle"))
                    .willReturn(updatedMember);

            // when & then
            mockMvc.perform(post("/api/member/me/verify-solvedac")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.handle").value("newHandle"));

            verify(memberService).verifySolvedAcHandle(1L, "newHandle");
        }

        @Test
        @WithMockUser
        @DisplayName("실패: solved.ac에 존재하지 않는 핸들")
        void verifySolvedAc_HandleNotFound() throws Exception {
            // given
            VerifySolvedAcRequest request = new VerifySolvedAcRequest("invalidHandle");
            given(memberService.verifySolvedAcHandle(1L, "invalidHandle"))
                    .willThrow(new CustomException(CustomResponseStatus.SOLVED_AC_USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/member/me/verify-solvedac")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(memberService).verifySolvedAcHandle(1L, "invalidHandle");
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 핸들이 null일 때")
        void verifySolvedAc_NullHandle() throws Exception {
            // given
            String requestJson = "{}";

            // when & then
            mockMvc.perform(post("/api/member/me/verify-solvedac")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/member/check-email - 이메일 중복 검사")
    class CheckEmailTests {

        @Test
        @DisplayName("성공: 사용 가능한 이메일")
        void checkEmail_Available() throws Exception {
            // given
            CheckEmailRequest request = new CheckEmailRequest("new@example.com");
            given(memberService.isEmailAvailable("new@example.com")).willReturn(true);

            // when & then
            mockMvc.perform(post("/api/member/check-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.available").value(true));

            verify(memberService).isEmailAvailable("new@example.com");
        }

        @Test
        @DisplayName("성공: 이미 사용 중인 이메일")
        void checkEmail_AlreadyExists() throws Exception {
            // given
            CheckEmailRequest request = new CheckEmailRequest("existing@example.com");
            given(memberService.isEmailAvailable("existing@example.com")).willReturn(false);

            // when & then
            mockMvc.perform(post("/api/member/check-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.available").value(false));

            verify(memberService).isEmailAvailable("existing@example.com");
        }

        @Test
        @DisplayName("실패: 잘못된 이메일 형식")
        void checkEmail_InvalidFormat() throws Exception {
            // given
            String requestJson = "{\"email\": \"invalid-email\"}";

            // when & then
            mockMvc.perform(post("/api/member/check-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/member/me/send-email-verification - 이메일 변경 인증 메일 발송")
    class SendEmailVerificationTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 이메일 변경 인증 메일을 발송한다")
        void sendEmailVerification_Success() throws Exception {
            // given
            SendEmailVerificationRequest request = new SendEmailVerificationRequest("newemail@example.com");

            // when & then
            mockMvc.perform(post("/api/member/me/send-email-verification")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));

            verify(memberService).sendEmailVerification(1L, "newemail@example.com");
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 이미 사용 중인 이메일")
        void sendEmailVerification_EmailAlreadyExists() throws Exception {
            // given
            SendEmailVerificationRequest request = new SendEmailVerificationRequest("existing@example.com");
            given(memberService.sendEmailVerification(eq(1L), anyString()))
                    .willThrow(new CustomException(CustomResponseStatus.EMAIL_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/api/member/me/send-email-verification")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 잘못된 이메일 형식")
        void sendEmailVerification_InvalidFormat() throws Exception {
            // given
            String requestJson = "{\"email\": \"invalid\"}";

            // when & then
            mockMvc.perform(post("/api/member/me/send-email-verification")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/member/verify-email - 이메일 변경 완료")
    class VerifyEmailTests {

        @Test
        @DisplayName("성공: 이메일 인증 토큰을 검증하고 이메일을 변경한다")
        void verifyEmail_Success() throws Exception {
            // given
            Member updatedMember = Member.builder()
                    .id(1L)
                    .email("newemail@example.com")
                    .handle("testHandle")
                    .isVerified(true)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("12345")
                    .build();

            VerifyEmailRequest request = new VerifyEmailRequest("valid-token");
            given(memberService.verifyAndChangeEmail("valid-token"))
                    .willReturn(updatedMember);

            // when & then
            mockMvc.perform(post("/api/member/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.email").value("newemail@example.com"));

            verify(memberService).verifyAndChangeEmail("valid-token");
        }

        @Test
        @DisplayName("실패: 만료된 토큰")
        void verifyEmail_ExpiredToken() throws Exception {
            // given
            VerifyEmailRequest request = new VerifyEmailRequest("expired-token");
            given(memberService.verifyAndChangeEmail("expired-token"))
                    .willThrow(new CustomException(CustomResponseStatus.EXPIRED_JWT));

            // when & then
            mockMvc.perform(post("/api/member/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패: 잘못된 토큰 타입")
        void verifyEmail_InvalidTokenType() throws Exception {
            // given
            VerifyEmailRequest request = new VerifyEmailRequest("invalid-type-token");
            given(memberService.verifyAndChangeEmail("invalid-type-token"))
                    .willThrow(new CustomException(CustomResponseStatus.INVALID_EMAIL_VERIFICATION_TOKEN));

            // when & then
            mockMvc.perform(post("/api/member/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 이메일이 다른 사용자에게 할당됨")
        void verifyEmail_EmailAlreadyTaken() throws Exception {
            // given
            VerifyEmailRequest request = new VerifyEmailRequest("valid-token");
            given(memberService.verifyAndChangeEmail("valid-token"))
                    .willThrow(new CustomException(CustomResponseStatus.EMAIL_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/api/member/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict());
        }
    }
}