    package com.ryu.studyhelper.recommendation.service;

    import com.ryu.studyhelper.infrastructure.mail.sender.MailSender;
    import com.ryu.studyhelper.recommendation.domain.member.EmailSendStatus;
    import com.ryu.studyhelper.recommendation.domain.member.MemberRecommendation;
    import com.ryu.studyhelper.recommendation.mailbuilder.RecommendationMailBuilder;
    import com.ryu.studyhelper.recommendation.repository.MemberRecommendationRepository;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.time.Clock;
    import java.time.LocalDateTime;
    import java.util.List;

    /**
     * 추천 이메일 발송
     * 배치(sendAll)와 수동(send) 모두 담당
     */
    @Service
    @RequiredArgsConstructor
    @Transactional
    @Slf4j
    public class RecommendationEmailService {

        private final Clock clock;
        private final MailSender mailSender;
        private final RecommendationMailBuilder recommendationMailBuilder;
        private final MemberRecommendationRepository memberRecommendationRepository;

        /**
         * 배치: PENDING 상태의 추천들에 대해 이메일 발송
         * 미션 사이클 기준(06:00~06:00)으로 조회
         */
        public void sendAll() {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime missionCycleStart = MissionCyclePolicy.getMissionCycleStart(clock);
            log.info("이메일 발송 배치 시작: {} (미션 사이클: {} 06:00 ~)", now.toLocalDate(), missionCycleStart.toLocalDate());

            List<MemberRecommendation> pendingRecommendations = memberRecommendationRepository
                    .findPendingRecommendationsByCreatedAtBetween(missionCycleStart, now, EmailSendStatus.PENDING);

            int successCount = 0;
            int failCount = 0;

            for (MemberRecommendation mr : pendingRecommendations) {
                if (sendEmail(mr)) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            log.info("이메일 발송 배치 완료 - 대상: {}개, 성공: {}개, 실패: {}개",
                    pendingRecommendations.size(), successCount, failCount);
        }

        /**
         * 수동 추천: 해당 추천의 팀원들에게 이메일 즉시 발송
         */
        public void send(List<MemberRecommendation> memberRecommendations) {
            for (MemberRecommendation mr : memberRecommendations) {
                sendEmail(mr);
            }
        }

        private boolean sendEmail(MemberRecommendation mr) {
            try {
                String email = mr.getMember().getEmail();
                if (email == null || email.isBlank()) {
                    mr.markEmailAsFailed();
                    memberRecommendationRepository.save(mr);
                    log.warn("회원 ID {}에 이메일이 없습니다", mr.getMember().getId());
                    return false;
                }

                mailSender.send(recommendationMailBuilder.build(mr));

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
