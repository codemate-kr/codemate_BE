package com.ryu.studyhelper.solve.service;

import com.ryu.studyhelper.solve.dto.projection.GlobalRankingProjection;
import com.ryu.studyhelper.solve.dto.response.GlobalRankingResponse;
import com.ryu.studyhelper.solve.repository.MemberSolvedProblemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService 단위 테스트")
class RankingServiceTest {

    @InjectMocks
    private RankingService rankingService;

    @Mock
    private MemberSolvedProblemRepository memberSolvedProblemRepository;

    @Nested
    @DisplayName("getGlobalRanking 메서드")
    class GetGlobalRankingTest {

        @Test
        @DisplayName("성공 - 전체 랭킹 조회")
        void success() {
            // given
            List<GlobalRankingProjection> projections = List.of(
                    createProjection("user1", 100L),
                    createProjection("user2", 80L),
                    createProjection("user3", 60L)
            );
            given(memberSolvedProblemRepository.findGlobalRanking(10)).willReturn(projections);

            // when
            GlobalRankingResponse response = rankingService.getGlobalRanking();

            // then
            assertThat(response.rankings()).hasSize(3);
            assertThat(response.rankings().get(0).rank()).isEqualTo(1);
            assertThat(response.rankings().get(0).handle()).isEqualTo("user1");
            assertThat(response.rankings().get(0).solvedCount()).isEqualTo(100L);
            assertThat(response.rankings().get(1).rank()).isEqualTo(2);
            assertThat(response.rankings().get(2).rank()).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 - 동점자 처리 (공동 2위)")
        void success_tieBreak() {
            // given
            List<GlobalRankingProjection> projections = List.of(
                    createProjection("user1", 100L),
                    createProjection("user2", 80L),
                    createProjection("user3", 80L),
                    createProjection("user4", 60L)
            );
            given(memberSolvedProblemRepository.findGlobalRanking(10)).willReturn(projections);

            // when
            GlobalRankingResponse response = rankingService.getGlobalRanking();

            // then
            assertThat(response.rankings()).hasSize(4);
            assertThat(response.rankings().get(0).rank()).isEqualTo(1); // user1: 1위
            assertThat(response.rankings().get(1).rank()).isEqualTo(2); // user2: 공동 2위
            assertThat(response.rankings().get(2).rank()).isEqualTo(2); // user3: 공동 2위
            assertThat(response.rankings().get(3).rank()).isEqualTo(4); // user4: 4위 (3위 건너뜀)
        }

        @Test
        @DisplayName("성공 - 전원 동점")
        void success_allTied() {
            // given
            List<GlobalRankingProjection> projections = List.of(
                    createProjection("aaa", 50L),
                    createProjection("bbb", 50L),
                    createProjection("ccc", 50L)
            );
            given(memberSolvedProblemRepository.findGlobalRanking(10)).willReturn(projections);

            // when
            GlobalRankingResponse response = rankingService.getGlobalRanking();

            // then
            assertThat(response.rankings()).hasSize(3);
            assertThat(response.rankings().get(0).rank()).isEqualTo(1);
            assertThat(response.rankings().get(1).rank()).isEqualTo(1);
            assertThat(response.rankings().get(2).rank()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 랭킹 데이터 없음")
        void success_emptyRanking() {
            // given
            given(memberSolvedProblemRepository.findGlobalRanking(10)).willReturn(List.of());

            // when
            GlobalRankingResponse response = rankingService.getGlobalRanking();

            // then
            assertThat(response.rankings()).isEmpty();
        }

        @Test
        @DisplayName("성공 - 0문제 푼 사용자 포함")
        void success_zeroSolved() {
            // given
            List<GlobalRankingProjection> projections = List.of(
                    createProjection("user1", 10L),
                    createProjection("user2", 0L),
                    createProjection("user3", 0L)
            );
            given(memberSolvedProblemRepository.findGlobalRanking(10)).willReturn(projections);

            // when
            GlobalRankingResponse response = rankingService.getGlobalRanking();

            // then
            assertThat(response.rankings()).hasSize(3);
            assertThat(response.rankings().get(0).rank()).isEqualTo(1);
            assertThat(response.rankings().get(0).solvedCount()).isEqualTo(10L);
            assertThat(response.rankings().get(1).rank()).isEqualTo(2); // 공동 2위
            assertThat(response.rankings().get(2).rank()).isEqualTo(2); // 공동 2위
        }

        private GlobalRankingProjection createProjection(String handle, Long totalSolved) {
            return new GlobalRankingProjection() {
                @Override
                public String getHandle() {
                    return handle;
                }

                @Override
                public Long getTotalSolved() {
                    return totalSolved;
                }
            };
        }
    }
}
