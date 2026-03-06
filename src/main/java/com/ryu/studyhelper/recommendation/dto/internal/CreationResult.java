package com.ryu.studyhelper.recommendation.dto.internal;

import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;

import java.util.List;

/**
 * 추천 생성 결과 (REQUIRES_NEW 커밋 후 DB 재조회 없이 문제/멤버 목록 전달)
 */
public record CreationResult(
        Recommendation recommendation,
        List<Problem> problems,
        List<MemberRecommendation> memberRecommendations
) {}
