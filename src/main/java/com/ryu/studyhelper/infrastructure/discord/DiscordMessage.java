package com.ryu.studyhelper.infrastructure.discord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Discord Embed 메시지 구조
 *
 * 사용법: 용도에 맞는 팩토리 메서드를 사용
 * - batchResult() : 배치 작업 결과 (스케줄러)
 * - error()       : 예외 발생 (스케줄러 실패)
 * - event()       : 비즈니스 이벤트 (가입, 핸들등록 등)
 */
public record DiscordMessage(List<Embed> embeds) {

    record Embed(String title, String description, int color, List<Field> fields, String timestamp) {}

    record Field(String name, String value, boolean inline) {}

    private static final int COLOR_SUCCESS = 0x2ECC71;
    private static final int COLOR_ERROR = 0xE74C3C;
    private static final int COLOR_INFO = 0x3498DB;

    /**
     * 배치 작업 결과
     * 실패가 있으면 빨간색, 없으면 초록색
     */
    public static DiscordMessage batchResult(String title, int totalCount, int successCount, int failCount, long elapsedMs) {
        int color = failCount > 0 ? COLOR_ERROR : COLOR_SUCCESS;
        return create(title, color, List.of(
                new Field("대상", totalCount + "건", true),
                new Field("성공", successCount + "건", true),
                new Field("실패", failCount + "건", true),
                new Field("소요시간", elapsedMs + "ms", true)
        ));
    }

    /**
     * 예외 발생 (빨간색)
     */
    public static DiscordMessage error(String title, Exception e, long elapsedMs) {
        return create(title, COLOR_ERROR, List.of(
                new Field("예외", e.getClass().getSimpleName(), true),
                new Field("메시지", e.getMessage() != null ? e.getMessage() : "(없음)", false),
                new Field("소요시간", elapsedMs + "ms", true)
        ));
    }

    /**
     * 비즈니스 이벤트 (파란색)
     * keyValues는 이름-값 쌍: "회원 ID", "1", "이메일", "test@..."
     */
    public static DiscordMessage event(String title, String... keyValues) {
        List<Field> fields = new ArrayList<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            fields.add(new Field(keyValues[i], keyValues[i + 1], true));
        }
        return create(title, COLOR_INFO, fields);
    }

    private static DiscordMessage create(String title, int color, List<Field> fields) {
        return new DiscordMessage(List.of(
                new Embed(title, null, color, fields, Instant.now().toString())
        ));
    }
}