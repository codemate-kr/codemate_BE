package com.ryu.studyhelper.solvedac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * solved.ac API 태그 응답 파싱용 DTO
 *
 * API 응답 예시:
 * {
 *   "key": "binary_search",
 *   "isMeta": false,
 *   "bojTagId": 12,
 *   "problemCount": 1137,
 *   "displayNames": [
 *     {"language": "ko", "name": "이분 탐색", "short": "이분 탐색"},
 *     {"language": "en", "name": "binary search", "short": "binary search"}
 *   ]
 * }
 */
public record SolvedAcTagInfo(
        @JsonProperty("key")
        String key,

        @JsonProperty("isMeta")
        boolean isMeta,

        @JsonProperty("bojTagId")
        Integer bojTagId,

        @JsonProperty("displayNames")
        List<DisplayName> displayNames
) {
    public record DisplayName(
            @JsonProperty("language")
            String language,

            @JsonProperty("name")
            String name,

            @JsonProperty("short")
            String shortName
    ) {}

    /**
     * 한글 태그명 추출
     */
    public String getNameKo() {
        if (displayNames == null) return null;
        return displayNames.stream()
                .filter(d -> "ko".equals(d.language()))
                .map(DisplayName::name)
                .findFirst()
                .orElse(null);
    }

    /**
     * 영문 태그명 추출
     */
    public String getNameEn() {
        if (displayNames == null) return null;
        return displayNames.stream()
                .filter(d -> "en".equals(d.language()))
                .map(DisplayName::name)
                .findFirst()
                .orElse(null);
    }
}