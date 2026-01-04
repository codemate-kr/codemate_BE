package com.ryu.studyhelper.member;

import com.ryu.studyhelper.member.dto.response.GlobalRankingResponse;
import com.ryu.studyhelper.member.repository.MemberSolvedProblemRepository;
import com.ryu.studyhelper.member.repository.MemberSolvedProblemRepository.GlobalRankingProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private static final int DEFAULT_RANKING_LIMIT = 10;

    private final MemberSolvedProblemRepository memberSolvedProblemRepository;

    /**
     * 전체 랭킹 조회 (상위 10명)
     */
    public GlobalRankingResponse getGlobalRanking() {
        List<GlobalRankingProjection> projections =
                memberSolvedProblemRepository.findGlobalRanking(DEFAULT_RANKING_LIMIT);

        List<GlobalRankingResponse.RankEntry> rankings = assignRanks(projections);

        return GlobalRankingResponse.from(rankings);
    }

    /**
     * 순위 부여 (동점자 처리: 공동 2위 2명이면 다음은 4위)
     */
    private List<GlobalRankingResponse.RankEntry> assignRanks(List<GlobalRankingProjection> projections) {
        List<GlobalRankingResponse.RankEntry> rankings = new ArrayList<>();

        int currentRank = 1;
        long previousSolved = -1;
        int sameRankCount = 0;

        for (GlobalRankingProjection projection : projections) {
            long totalSolved = projection.getTotalSolved();

            if (totalSolved != previousSolved) {
                currentRank += sameRankCount;
                sameRankCount = 1;
            } else {
                sameRankCount++;
            }

            rankings.add(new GlobalRankingResponse.RankEntry(
                    currentRank,
                    projection.getHandle(),
                    totalSolved
            ));

            previousSolved = totalSolved;
        }

        return rankings;
    }
}