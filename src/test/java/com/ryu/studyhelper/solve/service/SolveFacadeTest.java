package com.ryu.studyhelper.solve.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.solvedac.SolvedAcClient;
import com.ryu.studyhelper.solve.dto.response.DailySolvedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolveFacade 단위 테스트")
class SolveFacadeTest {

    @InjectMocks
    private SolveFacade solveFacade;

    @Mock
    private SolveService solveService;

    @Mock
    private SolvedAcClient solvedAcClient;

    @Test
    @DisplayName("성공 - 문제 해결 인증")
    void verifyProblemSolved_success() {
        given(solveService.validateAndGetHandle(1L, 1000L)).willReturn("testuser");
        given(solvedAcClient.hasUserSolvedProblem("testuser", 1000L)).willReturn(true);

        solveFacade.verifyProblemSolved(1L, 1000L);

        verify(solveService).recordSolved(1L, 1000L);
    }

    @Test
    @DisplayName("실패 - solved.ac에서 아직 풀이 안 됨")
    void verifyProblemSolved_fail_notSolvedYet() {
        given(solveService.validateAndGetHandle(1L, 1000L)).willReturn("testuser");
        given(solvedAcClient.hasUserSolvedProblem("testuser", 1000L)).willReturn(false);

        assertThatThrownBy(() -> solveFacade.verifyProblemSolved(1L, 1000L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.PROBLEM_NOT_SOLVED_YET);

        verify(solveService, never()).recordSolved(1L, 1000L);
    }

    @Test
    @DisplayName("실패 - 사전 검증 예외 전파")
    void verifyProblemSolved_fail_precondition() {
        given(solveService.validateAndGetHandle(1L, 1000L))
                .willThrow(new CustomException(CustomResponseStatus.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> solveFacade.verifyProblemSolved(1L, 1000L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("status", CustomResponseStatus.MEMBER_NOT_FOUND);        verifyNoInteractions(solvedAcClient);
        verify(solveService, never()).recordSolved(1L, 1000L);
    }

    @Test
    @DisplayName("성공 - 일별 풀이 조회 위임")
    void getDailySolved_delegate() {
        DailySolvedResponse response = new DailySolvedResponse(List.of(), 0);
        given(solveService.getDailySolved(1L, 7)).willReturn(response);

        DailySolvedResponse actual = solveFacade.getDailySolved(1L, 7);

        assertThat(actual).isSameAs(response);
    }
}
