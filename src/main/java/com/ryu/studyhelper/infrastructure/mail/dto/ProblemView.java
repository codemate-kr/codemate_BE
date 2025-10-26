package com.ryu.studyhelper.infrastructure.mail.dto;

/**
 * 이메일 템플릿에서 사용되는 문제 정보 뷰 모델
 *
 * Thymeleaf 템플릿에서 문제 정보를 표시하기 위한 DTO
 * - 티어 색상 정보를 포함하여 이메일 클라이언트에서 스타일 적용
 * - 프론트엔드와 동일한 티어 색상 시스템 사용
 */
public class ProblemView {
    private final Integer order;
    private final String title;
    private final Integer level;
    private final String url;
    private final Long problemId;
    private final Integer acceptedUserCount;
    private final Double averageTries;
    private final Boolean solved;

    // 티어 정보 (1-30 레벨)
    private static final String[] TIER_NAMES = {
        "Bronze V", "Bronze IV", "Bronze III", "Bronze II", "Bronze I",
        "Silver V", "Silver IV", "Silver III", "Silver II", "Silver I",
        "Gold V", "Gold IV", "Gold III", "Gold II", "Gold I",
        "Platinum V", "Platinum IV", "Platinum III", "Platinum II", "Platinum I",
        "Diamond V", "Diamond IV", "Diamond III", "Diamond II", "Diamond I",
        "Ruby V", "Ruby IV", "Ruby III", "Ruby II", "Ruby I"
    };

    // 티어별 텍스트 색상 (프론트엔드와 동일)
    private static final String[] TIER_COLORS = {
        "#ea580c", "#ea580c", "#ea580c", "#ea580c", "#ea580c",  // Bronze (text-orange-600)
        "#6b7280", "#6b7280", "#6b7280", "#6b7280", "#6b7280",  // Silver (text-gray-500)
        "#ca8a04", "#ca8a04", "#ca8a04", "#ca8a04", "#ca8a04",  // Gold (text-yellow-600)
        "#0891b2", "#0891b2", "#0891b2", "#0891b2", "#0891b2",  // Platinum (text-cyan-600)
        "#2563eb", "#2563eb", "#2563eb", "#2563eb", "#2563eb",  // Diamond (text-blue-600)
        "#dc2626", "#dc2626", "#dc2626", "#dc2626", "#dc2626"   // Ruby (text-red-600)
    };

    // 티어별 배경색 (프론트엔드와 동일)
    private static final String[] TIER_BG_COLORS = {
        "#fff7ed", "#fff7ed", "#fff7ed", "#fff7ed", "#fff7ed",  // Bronze (bg-orange-50)
        "#f3f4f6", "#f3f4f6", "#f3f4f6", "#f3f4f6", "#f3f4f6",  // Silver (bg-gray-100)
        "#fefce8", "#fefce8", "#fefce8", "#fefce8", "#fefce8",  // Gold (bg-yellow-50)
        "#ecfeff", "#ecfeff", "#ecfeff", "#ecfeff", "#ecfeff",  // Platinum (bg-cyan-50)
        "#eff6ff", "#eff6ff", "#eff6ff", "#eff6ff", "#eff6ff",  // Diamond (bg-blue-50)
        "#fef2f2", "#fef2f2", "#fef2f2", "#fef2f2", "#fef2f2"   // Ruby (bg-red-50)
    };

    public ProblemView(Integer order, String title, Integer level, String url,
                      Long problemId, Integer acceptedUserCount, Double averageTries, Boolean solved) {
        this.order = order;
        this.title = title;
        this.level = level;
        this.url = url;
        this.problemId = problemId;
        this.acceptedUserCount = acceptedUserCount != null ? acceptedUserCount : 0;
        this.averageTries = averageTries != null ? averageTries : 0.0;
        this.solved = solved != null ? solved : false;
    }

    public Integer getOrder() { return order; }
    public String getTitle() { return title; }
    public Integer getLevel() { return level; }
    public String getUrl() { return url; }
    public Long getProblemId() { return problemId; }
    public Integer getAcceptedUserCount() { return acceptedUserCount; }
    public Double getAverageTries() { return averageTries; }
    public Boolean getSolved() { return solved; }

    /**
     * 평균 시도 횟수를 소수점 1자리까지 포맷팅하여 반환
     */
    public String getFormattedAverageTries() {
        return String.format("%.1f", averageTries);
    }

    /**
     * 티어 이름 반환 (예: "Gold III")
     */
    public String getTierName() {
        if (level == null || level <= 0 || level > 30) {
            return "Unrated";
        }
        return TIER_NAMES[level - 1];
    }

    /**
     * 티어 텍스트 색상 반환 (프론트엔드 getTierColor의 text-* 부분)
     */
    public String getTierColor() {
        if (level == null || level <= 0 || level > 30) {
            return "#6b7280";  // gray-500
        }
        return TIER_COLORS[level - 1];
    }

    /**
     * 티어 배경색 반환 (프론트엔드 getTierColor의 bg-* 부분)
     */
    public String getTierBgColor() {
        if (level == null || level <= 0 || level > 30) {
            return "#f3f4f6";  // gray-100
        }
        return TIER_BG_COLORS[level - 1];
    }
}
