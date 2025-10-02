package com.ryu.studyhelper.team.domain;

import lombok.Getter;

/**
 * 문제 난이도 프리셋 enum
 * 프론트엔드에서 제공하는 4가지 선택지
 */
@Getter
public enum ProblemDifficultyPreset {

    EASY("쉬움", 5, 8),         // 브론즈 1 ~ 실버 3
    NORMAL("보통", 9, 12),      // 실버 2 ~ 골드 4
    HARD("어려움", 13, 16),     // 골드 3 ~ 플래티넘 5
    CUSTOM("커스텀", null, null); // 사용자 정의

    private final String displayName;
    private final Integer minLevel;
    private final Integer maxLevel;

    ProblemDifficultyPreset(String displayName, Integer minLevel, Integer maxLevel) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /**
     * 프리셋이 커스텀인지 확인
     */
    public boolean isCustom() {
        return this == CUSTOM;
    }

    /**
     * 프리셋에 해당하는 난이도 범위가 있는지 확인
     */
    public boolean hasLevelRange() {
        return minLevel != null && maxLevel != null;
    }
}