package com.ryu.studyhelper.common.util;

/**
 * BOJ 문제 URL 생성 유틸리티
 */
public final class ProblemUrlUtils {

    private static final String BOJ_PROBLEM_BASE_URL = "https://www.acmicpc.net/problem/";

    private ProblemUrlUtils() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    public static String generateProblemUrl(Long problemId) {
        if (problemId == null) {
            throw new IllegalArgumentException("problemId는 null일 수 없습니다");
        }
        return BOJ_PROBLEM_BASE_URL + problemId;
    }
}