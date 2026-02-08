package com.ryu.studyhelper.infrastructure.mail.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.nio.charset.StandardCharsets;

/**
 * AWS SES 기반 메일 발송 구현체
 */
@Component
@Slf4j
public class SesMailSender implements MailSender {

    private static final String SENDER_NAME = "CodeMate";

    private final SesClient sesClient;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.configuration-set:#{null}}")
    private String configurationSetName;

    public SesMailSender(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    @Override
    public void send(MailMessage message) {
        try {
            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .source(SENDER_NAME + " <" + fromEmail + ">")
                    .destination(Destination.builder().toAddresses(message.to()).build())
                    .message(Message.builder()
                            .subject(createContent(message.subject()))
                            .body(Body.builder()
                                    .html(createContent(message.html()))
                                    .build())
                            .build());

            if (configurationSetName != null && !configurationSetName.isBlank()) {
                requestBuilder.configurationSetName(configurationSetName);
            }

            sesClient.sendEmail(requestBuilder.build());
            log.debug("이메일 발송 완료: {}", message.to());
        } catch (SesException e) {
            log.error("이메일 전송 실패 ({}): {}", message.to(), e.getMessage());
            throw new RuntimeException("이메일 전송 실패: " + e.getMessage(), e);
        }
    }

    private Content createContent(String data) {
        return Content.builder()
                .data(data)
                .charset(StandardCharsets.UTF_8.name())
                .build();
    }
}