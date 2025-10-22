package com.ryu.studyhelper.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;
import com.ryu.studyhelper.team.domain.ProblemDifficultyPreset;
import com.ryu.studyhelper.team.domain.RecommendationDayOfWeek;
import com.ryu.studyhelper.team.dto.*;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("TeamController 단위 테스트")
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamService teamService;

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
    @DisplayName("GET /api/teams/my - 내가 속한 팀 목록 조회")
    class GetMyTeamsTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 내가 속한 팀 목록을 조회한다")
        void getMyTeams_Success() throws Exception {
            // given
            List<MyTeamResponse> responses = Arrays.asList(
                    new MyTeamResponse(1L, "Team 1", "Description 1", "LEADER"),
                    new MyTeamResponse(2L, "Team 2", "Description 2", "MEMBER")
            );
            given(teamService.getMyTeams(1L)).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/teams/my")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].teamId").value(1L))
                    .andExpect(jsonPath("$.data[0].name").value("Team 1"))
                    .andExpect(jsonPath("$.data[1].teamId").value(2L));

            verify(teamService).getMyTeams(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 소속된 팀이 없을 때 빈 배열 반환")
        void getMyTeams_EmptyList() throws Exception {
            // given
            given(teamService.getMyTeams(1L)).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/teams/my")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(teamService).getMyTeams(1L);
        }
    }

    @Nested
    @DisplayName("POST /api/teams - 팀 생성")
    class CreateTeamTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 새로운 팀을 생성한다")
        void createTeam_Success() throws Exception {
            // given
            CreateTeamRequest request = new CreateTeamRequest("New Team", "Team Description");
            CreateTeamResponse response = CreateTeamResponse.from("New Team", "Team Description");
            given(teamService.create(any(CreateTeamRequest.class), eq(1L)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/teams")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.name").value("New Team"))
                    .andExpect(jsonPath("$.data.description").value("Team Description"));

            verify(teamService).create(any(CreateTeamRequest.class), eq(1L));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 팀 이름이 없을 때")
        void createTeam_MissingName() throws Exception {
            // given
            String requestJson = "{\"description\": \"Description only\"}";

            // when & then
            mockMvc.perform(post("/api/teams")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}/members - 팀 멤버 목록 조회")
    class GetTeamMembersTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 팀 멤버 목록을 조회한다")
        void getTeamMembers_Success() throws Exception {
            // given
            List<TeamMemberResponse> responses = Arrays.asList(
                    new TeamMemberResponse(1L, 1L, "user1@example.com", "handle1", "LEADER", true),
                    new TeamMemberResponse(2L, 2L, "user2@example.com", "handle2", "MEMBER", false)
            );
            given(teamService.getTeamMembers(1L, 1L)).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/teams/1/members")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].role").value("LEADER"))
                    .andExpect(jsonPath("$.data[1].role").value("MEMBER"));

            verify(teamService).getTeamMembers(1L, 1L);
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 존재하지 않는 팀")
        void getTeamMembers_TeamNotFound() throws Exception {
            // given
            given(teamService.getTeamMembers(999L, 1L))
                    .willThrow(new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/teams/999/members")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{teamId}/recommendation-settings - 팀 추천 설정 조회")
    class GetRecommendationSettingsTests {

        @Test
        @DisplayName("성공: 팀 추천 설정을 조회한다")
        void getRecommendationSettings_Success() throws Exception {
            // given
            TeamRecommendationSettingsResponse response = new TeamRecommendationSettingsResponse(
                    Set.of(RecommendationDayOfWeek.MONDAY, RecommendationDayOfWeek.WEDNESDAY),
                    ProblemDifficultyPreset.NORMAL,
                    null,
                    null
            );
            given(teamService.getRecommendationSettings(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/teams/1/recommendation-settings"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.problemDifficultyPreset").value("NORMAL"));

            verify(teamService).getRecommendationSettings(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 팀")
        void getRecommendationSettings_TeamNotFound() throws Exception {
            // given
            given(teamService.getRecommendationSettings(999L))
                    .willThrow(new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/teams/999/recommendation-settings"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/teams/{teamId}/recommendation-settings - 팀 추천 설정 업데이트")
    class UpdateRecommendationSettingsTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 팀 추천 설정을 업데이트한다 (프리셋 모드)")
        void updateRecommendationSettings_Success_PresetMode() throws Exception {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY, RecommendationDayOfWeek.FRIDAY),
                    ProblemDifficultyPreset.HARD,
                    null,
                    null
            );
            TeamRecommendationSettingsResponse response = new TeamRecommendationSettingsResponse(
                    Set.of(RecommendationDayOfWeek.MONDAY, RecommendationDayOfWeek.FRIDAY),
                    ProblemDifficultyPreset.HARD,
                    null,
                    null
            );
            given(teamService.updateRecommendationSettings(eq(1L), any(), eq(1L)))
                    .willReturn(response);
            given(teamService.isTeamLeader(1L, 1L)).willReturn(true);

            // when & then
            mockMvc.perform(put("/api/teams/1/recommendation-settings")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.problemDifficultyPreset").value("HARD"));

            verify(teamService).updateRecommendationSettings(eq(1L), any(), eq(1L));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 팀 추천 설정을 업데이트한다 (커스텀 모드)")
        void updateRecommendationSettings_Success_CustomMode() throws Exception {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.TUESDAY),
                    ProblemDifficultyPreset.CUSTOM,
                    5,
                    15
            );
            TeamRecommendationSettingsResponse response = new TeamRecommendationSettingsResponse(
                    Set.of(RecommendationDayOfWeek.TUESDAY),
                    ProblemDifficultyPreset.CUSTOM,
                    5,
                    15
            );
            given(teamService.updateRecommendationSettings(eq(1L), any(), eq(1L)))
                    .willReturn(response);
            given(teamService.isTeamLeader(1L, 1L)).willReturn(true);

            // when & then
            mockMvc.perform(put("/api/teams/1/recommendation-settings")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.problemDifficultyPreset").value("CUSTOM"))
                    .andExpect(jsonPath("$.data.customMinLevel").value(5))
                    .andExpect(jsonPath("$.data.customMaxLevel").value(15));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 최소 난이도가 최대 난이도보다 큼")
        void updateRecommendationSettings_InvalidLevelRange() throws Exception {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY),
                    ProblemDifficultyPreset.CUSTOM,
                    20,
                    10
            );
            given(teamService.updateRecommendationSettings(eq(1L), any(), eq(1L)))
                    .willThrow(new CustomException(CustomResponseStatus.INVALID_PROBLEM_LEVEL_RANGE));
            given(teamService.isTeamLeader(1L, 1L)).willReturn(true);

            // when & then
            mockMvc.perform(put("/api/teams/1/recommendation-settings")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 팀장이 아닌 사용자")
        void updateRecommendationSettings_NotTeamLeader() throws Exception {
            // given
            TeamRecommendationSettingsRequest request = new TeamRecommendationSettingsRequest(
                    Set.of(RecommendationDayOfWeek.MONDAY),
                    ProblemDifficultyPreset.NORMAL,
                    null,
                    null
            );
            given(teamService.isTeamLeader(1L, 1L)).willReturn(false);

            // when & then
            mockMvc.perform(put("/api/teams/1/recommendation-settings")
                            .with(user(principalDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/teams/{teamId}/recommendation-settings - 팀 추천 비활성화")
    class DisableRecommendationTests {

        @Test
        @WithMockUser
        @DisplayName("성공: 팀 추천을 비활성화한다")
        void disableRecommendation_Success() throws Exception {
            // given
            TeamRecommendationSettingsResponse response = new TeamRecommendationSettingsResponse(
                    Set.of(),
                    ProblemDifficultyPreset.NORMAL,
                    null,
                    null
            );
            given(teamService.disableRecommendation(1L, 1L)).willReturn(response);
            given(teamService.isTeamLeader(1L, 1L)).willReturn(true);

            // when & then
            mockMvc.perform(delete("/api/teams/1/recommendation-settings")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.recommendationDays").isEmpty());

            verify(teamService).disableRecommendation(1L, 1L);
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 팀장이 아닌 사용자")
        void disableRecommendation_NotTeamLeader() throws Exception {
            // given
            given(teamService.isTeamLeader(1L, 1L)).willReturn(false);

            // when & then
            mockMvc.perform(delete("/api/teams/1/recommendation-settings")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 존재하지 않는 팀")
        void disableRecommendation_TeamNotFound() throws Exception {
            // given
            given(teamService.disableRecommendation(999L, 1L))
                    .willThrow(new CustomException(CustomResponseStatus.TEAM_NOT_FOUND));
            given(teamService.isTeamLeader(999L, 1L)).willReturn(true);

            // when & then
            mockMvc.perform(delete("/api/teams/999/recommendation-settings")
                            .with(user(principalDetails)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}