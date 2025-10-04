package com.ryu.studyhelper.solvedac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 백준 핸들 인증용 DTO
 * solved.ac API에서 사용자의 bio(상태 메시지)만 가져옴
 */
public record BojVerificationDto(
        @JsonProperty("handle") String handle,
        @JsonProperty("bio") String bio
) {}