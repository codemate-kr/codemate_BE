package com.ryu.studyhelper.notification.email;

import com.ryu.studyhelper.notification.email.dto.MailHtmlSendDto;
import com.ryu.studyhelper.notification.email.dto.MailTxtSendDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/email")
public class MailSendController {

    private final MailSendService mailSendService;

    public MailSendController(MailSendService mailSendService) {
        this.mailSendService = mailSendService;
    }

    @PostMapping("/txtEmail")
    public ResponseEntity<ApiResponse<Void>> sendTxtEmail(@RequestBody MailTxtSendDto mailTxtSendDto) {
        mailSendService.sendTxtEmail(mailTxtSendDto);
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

    @PostMapping("/htmlEmail")
    public ResponseEntity<ApiResponse<Void>> sendHtmlEmail(@RequestBody MailHtmlSendDto mailHtmlSendDto) {
        mailSendService.sendHtmlEmail(mailHtmlSendDto);
        return ResponseEntity.ok(ApiResponse.createSuccess(null, CustomResponseStatus.SUCCESS));
    }

}