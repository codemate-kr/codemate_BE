package com.ryu.studyhelper.solvedac.dto;

public record SolvedAcVerificationResponse(
        String handle,
        String bio // 상태메시지/자기소개 텍스트 필드 (null 가능)
) {

}
