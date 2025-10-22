package com.ryu.studyhelper.member.dto;

import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberSearchResponse DTO 단위 테스트")
class MemberSearchResponseTest {

    @Nested
    @DisplayName("from() - Member 엔티티로부터 DTO 생성")
    class FromTests {

        @Test
        @DisplayName("성공: 인증된 회원의 경우 이메일을 포함한다")
        void from_VerifiedMember_IncludesEmail() {
            // given
            Member verifiedMember = Member.builder()
                    .id(1L)
                    .email("verified@example.com")
                    .handle("verifiedHandle")
                    .isVerified(true)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("12345")
                    .build();

            // when
            MemberSearchResponse response = MemberSearchResponse.from(verifiedMember);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.handle()).isEqualTo("verifiedHandle");
            assertThat(response.verified()).isTrue();
            assertThat(response.email()).isEqualTo("verified@example.com");
        }

        @Test
        @DisplayName("성공: 미인증 회원의 경우 이메일을 null로 반환한다 (변경된 동작)")
        void from_UnverifiedMember_EmailIsNull() {
            // given
            Member unverifiedMember = Member.builder()
                    .id(2L)
                    .email("unverified@example.com")
                    .handle("unverifiedHandle")
                    .isVerified(false)
                    .role(Role.ROLE_USER)
                    .provider("kakao")
                    .providerId("67890")
                    .build();

            // when
            MemberSearchResponse response = MemberSearchResponse.from(unverifiedMember);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(2L);
            assertThat(response.handle()).isEqualTo("unverifiedHandle");
            assertThat(response.verified()).isFalse();
            assertThat(response.email()).isNull(); // 변경된 부분: 미인증 회원은 이메일이 null
        }

        @Test
        @DisplayName("성공: 핸들이 없는 회원")
        void from_MemberWithoutHandle() {
            // given
            Member memberWithoutHandle = Member.builder()
                    .id(3L)
                    .email("nohandle@example.com")
                    .handle(null)
                    .isVerified(false)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("99999")
                    .build();

            // when
            MemberSearchResponse response = MemberSearchResponse.from(memberWithoutHandle);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(3L);
            assertThat(response.handle()).isNull();
            assertThat(response.verified()).isFalse();
            assertThat(response.email()).isNull();
        }

        @Test
        @DisplayName("성공: 여러 회원을 변환하여 리스트로 생성")
        void from_MultipleMembers() {
            // given
            Member member1 = Member.builder()
                    .id(1L)
                    .email("user1@example.com")
                    .handle("sameHandle")
                    .isVerified(true)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("11111")
                    .build();

            Member member2 = Member.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .handle("sameHandle")
                    .isVerified(false)
                    .role(Role.ROLE_USER)
                    .provider("kakao")
                    .providerId("22222")
                    .build();

            // when
            MemberSearchResponse response1 = MemberSearchResponse.from(member1);
            MemberSearchResponse response2 = MemberSearchResponse.from(member2);

            // then
            assertThat(response1.handle()).isEqualTo("sameHandle");
            assertThat(response2.handle()).isEqualTo("sameHandle");
            assertThat(response1.email()).isEqualTo("user1@example.com"); // verified
            assertThat(response2.email()).isNull(); // not verified
        }
    }

    @Nested
    @DisplayName("이메일 노출 정책 테스트")
    class EmailExposurePolicyTests {

        @Test
        @DisplayName("검증된 사용자만 이메일이 노출된다")
        void emailExposedOnlyForVerifiedUsers() {
            // given
            Member verifiedMember = Member.builder()
                    .id(1L)
                    .email("verified@example.com")
                    .handle("handle1")
                    .isVerified(true)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("12345")
                    .build();

            Member unverifiedMember = Member.builder()
                    .id(2L)
                    .email("unverified@example.com")
                    .handle("handle2")
                    .isVerified(false)
                    .role(Role.ROLE_USER)
                    .provider("google")
                    .providerId("67890")
                    .build();

            // when
            MemberSearchResponse verifiedResponse = MemberSearchResponse.from(verifiedMember);
            MemberSearchResponse unverifiedResponse = MemberSearchResponse.from(unverifiedMember);

            // then
            assertThat(verifiedResponse.verified()).isTrue();
            assertThat(verifiedResponse.email()).isNotNull();
            
            assertThat(unverifiedResponse.verified()).isFalse();
            assertThat(unverifiedResponse.email()).isNull();
        }
    }
}