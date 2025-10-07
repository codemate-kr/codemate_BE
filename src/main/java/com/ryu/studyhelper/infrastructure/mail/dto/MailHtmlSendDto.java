package com.ryu.studyhelper.infrastructure.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailHtmlSendDto {

    private String emailAddr;      // 수신자 이메일
    private String subject;        // 이메일 제목
    private String content;        // 이메일 내용(plain text 권장)
    private String target;         // 이메일 대상 타겟 (user/admin 등)

    // 선택: CTA 버튼 렌더링용 필드
    private String buttonUrl;      // 버튼 클릭 URL (옵션)
    private String buttonText;     // 버튼 텍스트 (옵션)

    // 선택: 사용할 템플릿 이름 (기본값: email-template)
    private String templateName;   // 예) "email-change-template"
}
