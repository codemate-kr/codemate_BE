package com.ryu.studyhelper.test;

import com.ryu.studyhelper.recommendation.scheduler.EmailSendScheduler;
import com.ryu.studyhelper.recommendation.scheduler.ProblemRecommendationScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/scheduler")
@RequiredArgsConstructor
@Profile({"local", "test"})
@Tag(name = "Test", description = "테스트용 API (local/test 환경에서만 활성화)")
public class TestSchedulerController {

    private final ProblemRecommendationScheduler recommendationScheduler;
    private final EmailSendScheduler emailSendScheduler;

    @PostMapping("/recommendation")
    @Operation(summary = "문제 추천 배치 수동 실행", description = "ProblemRecommendationScheduler를 수동으로 트리거합니다.")
    public String triggerRecommendation() {
        recommendationScheduler.prepareDailyRecommendations();
        return "문제 추천 배치 완료";
    }

    @PostMapping("/email")
    @Operation(summary = "이메일 발송 배치 수동 실행", description = "EmailSendScheduler를 수동으로 트리거합니다.")
    public String triggerEmail() {
        emailSendScheduler.sendPendingEmails();
        return "이메일 발송 배치 완료";
    }
}
