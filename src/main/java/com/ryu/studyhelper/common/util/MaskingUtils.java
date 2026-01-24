package com.ryu.studyhelper.common.util;

/**
 * 민감 정보 마스킹 유틸리티
 */
public final class MaskingUtils {

    private MaskingUtils() {
    }

    /**
     * 이메일 주소를 마스킹합니다.
     * 예: user@example.com -> u****@example.com
     *     ab@example.com -> ****@example.com (4자 이하인 경우)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 4) {
            return "****@" + domain;
        }

        String visiblePart = localPart.substring(0, localPart.length() - 4);
        return visiblePart + "****@" + domain;
    }
}
