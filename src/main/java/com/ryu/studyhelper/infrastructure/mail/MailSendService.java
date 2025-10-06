package com.ryu.studyhelper.infrastructure.mail;

import com.ryu.studyhelper.infrastructure.mail.dto.MailHtmlSendDto;
import com.ryu.studyhelper.infrastructure.mail.dto.MailTxtSendDto;
import com.ryu.studyhelper.recommendation.domain.TeamRecommendation;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * https://github.com/adjh54ir/blog-codes/tree/main/spring-boot-mail
 * 메일 전송 서비스 인터페이스
 */
@Service
public interface MailSendService {
    void sendTxtEmail(MailTxtSendDto mailTxtSendDto);       // SimpleMailMessage를 활용하여 텍스트 기반 메일을 전송합니다.

    void sendHtmlEmail(MailHtmlSendDto mailHtmlSendDto);

    /**
     * 팀 추천 이메일 발송
     */
    void sendRecommendationEmail(TeamRecommendation recommendation, List<String> memberEmails);
}