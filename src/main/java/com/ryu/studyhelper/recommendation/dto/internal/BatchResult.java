package com.ryu.studyhelper.recommendation.dto.internal;

/**
 * 배치 작업 결과
 */
public record BatchResult(int totalCount, int successCount, int failCount) {}
