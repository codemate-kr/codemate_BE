package com.ryu.studyhelper.recommendation.service;

import com.ryu.studyhelper.common.MissionCyclePolicy;
import com.ryu.studyhelper.infrastructure.mail.sender.MailSender;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.recommendation.domain.RecommendationProblem;
import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
import com.ryu.studyhelper.recommendation.dto.internal.BatchResult;
import com.ryu.studyhelper.recommendation.mailbuilder.RecommendationMailBuilder;
import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
import com.ryu.studyhelper.recommendation.repository.RecommendationProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 추천 이메일 발송
 * 배치(sendAll)와 수동(send) 모두 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationEmailService {

    private final Clock clock;
    private final MailSender mailSender;
    private final RecommendationMailBuilder recommendationMailBuilder;
    private final MemberRecommendationRepository memberRecommendationRepository;
    private final RecommendationProblemRepository recommendationProblemRepository;

    /**
     * 배치: PENDING 상태의 추천들에 대해 이메일 발송
     * 미션 사이클 기준(06:00~06:00)으로 조회
     */
    public BatchResult sendAll() {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        log.info("이메일 발송 배치 시작: {}", missionDate);

        List<MemberRecommendation> pendingRecommendations = memberRecommendationRepository
                .findByRecommendationDateAndEmailSendStatus(missionDate, EmailSendStatus.PENDING);

        Map<Long, List<Problem>> problemsByRecommendationId = loadProblemsByRecommendation(pendingRecommendations);

        int successCount = 0;
        int failCount = 0;

        for (MemberRecommendation mr : pendingRecommendations) {
            List<Problem> problems = problemsByRecommendationId.get(mr.getRecommendation().getId());
            if (sendEmail(mr, problems)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("이메일 발송 배치 완료 — 대상: {}개, 성공: {}개, 실패: {}개",
                pendingRecommendations.size(), successCount, failCount);
        return new BatchResult(pendingRecommendations.size(), successCount, 0, failCount);
    }

    /**
     * FAILED 이메일 재발송 배치.
     * 수동 트리거와 배치가 동시에 실행될 수 있어 FAILED → PENDING CAS로 선점 후 발송한다.
     * 문제 추천 재시도(RecommendationBatchService.retryFailed)와 동일한 패턴이다.
     */
    public BatchResult retryFailed() {
        LocalDate missionDate = MissionCyclePolicy.getMissionDate(clock);
        log.info("이메일 재발송 배치 시작: {}", missionDate);

        List<MemberRecommendation> failedRecommendations = memberRecommendationRepository
                .findByRecommendationDateAndEmailSendStatus(missionDate, EmailSendStatus.FAILED);

        Map<Long, List<Problem>> problemsByRecommendationId = loadProblemsByRecommendation(failedRecommendations);

        int successCount = 0;
        int failCount = 0;

        for (MemberRecommendation mr : failedRecommendations) {
            int claimed = memberRecommendationRepository.compareAndUpdateEmailSendStatus(
                    mr.getId(), EmailSendStatus.PENDING, EmailSendStatus.FAILED);
            if (claimed == 0) {
                log.info("회원 ID {} 재발송 스킵 — 다른 워커가 선점함", mr.getMember().getId());
                continue;
            }
            mr.retryAsPending();
            List<Problem> problems = problemsByRecommendationId.get(mr.getRecommendation().getId());
            if (sendEmail(mr, problems)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("이메일 재발송 배치 완료 — 대상: {}개, 성공: {}개, 실패: {}개",
                failedRecommendations.size(), successCount, failCount);
        return new BatchResult(failedRecommendations.size(), successCount, 0, failCount);
    }

    private Map<Long, List<Problem>> loadProblemsByRecommendation(List<MemberRecommendation> memberRecommendations) {
        return memberRecommendations.stream()
                .map(mr -> mr.getRecommendation().getId())
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> recommendationProblemRepository.findByRecommendationIdOrderById(id)
                                .stream().map(RecommendationProblem::getProblem).toList()
                ));
    }

    /**
     * 수동 추천: 해당 추천의 팀원들에게 이메일 즉시 발송.
     * problems는 CreationResult에서 직접 전달 — lazy 로딩 없이 사용.
     */
    public void send(List<MemberRecommendation> memberRecommendations, List<Problem> problems) {
        for (MemberRecommendation mr : memberRecommendations) {
            sendEmail(mr, problems);
        }
    }

    private boolean sendEmail(MemberRecommendation mr, List<Problem> problems) {
        try {
            String email = mr.getMember().getEmail();
            if (email == null || email.isBlank()) {
                mr.markEmailAsFailed();
                memberRecommendationRepository.save(mr);
                log.warn("회원 ID {}에 이메일이 없습니다", mr.getMember().getId());
                return false;
            }

            mailSender.send(recommendationMailBuilder.build(mr, problems));

            mr.markEmailAsSent();
            memberRecommendationRepository.save(mr);
            log.debug("회원 '{}' 이메일 발송 완료", mr.getMember().getHandle());
            return true;

        } catch (Exception e) {
            mr.markEmailAsFailed();
            memberRecommendationRepository.save(mr);
            log.error("회원 ID {} 이메일 발송 실패", mr.getMember().getId(), e);
            return false;
        }
    }
}