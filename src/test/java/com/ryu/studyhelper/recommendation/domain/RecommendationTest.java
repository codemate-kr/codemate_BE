package com.ryu.studyhelper.recommendation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Recommendation 상태머신 테스트")
class RecommendationTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 1, 15);

    private Recommendation pendingRec() {
        return Recommendation.createPending(1L, 10L, RecommendationType.SCHEDULED, TODAY);
    }

    @Nested
    @DisplayName("createPending")
    class CreatePending {

        @Test
        @DisplayName("생성 시 status는 PENDING이다")
        void initialStatusIsPending() {
            Recommendation rec = pendingRec();
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("markAsSuccess")
    class MarkAsSuccess {

        @Test
        @DisplayName("PENDING → SUCCESS 전이 성공")
        void pendingToSuccess() {
            Recommendation rec = pendingRec();
            rec.markAsSuccess();
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.SUCCESS);
        }

        @Test
        @DisplayName("FAILED 상태에서 SUCCESS 전이 시 예외")
        void failedToSuccess_throws() {
            Recommendation rec = pendingRec();
            rec.markAsFailed();

            assertThatThrownBy(rec::markAsSuccess)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }

        @Test
        @DisplayName("SUCCESS 상태에서 재전이 시 예외 (terminal)")
        void successToSuccess_throws() {
            Recommendation rec = pendingRec();
            rec.markAsSuccess();

            assertThatThrownBy(rec::markAsSuccess)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }
    }

    @Nested
    @DisplayName("markAsFailed")
    class MarkAsFailed {

        @Test
        @DisplayName("PENDING → FAILED 전이 성공")
        void pendingToFailed() {
            Recommendation rec = pendingRec();
            rec.markAsFailed();
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.FAILED);
        }

        @Test
        @DisplayName("SUCCESS 상태에서 FAILED 전이 시 예외")
        void successToFailed_throws() {
            Recommendation rec = pendingRec();
            rec.markAsSuccess();

            assertThatThrownBy(rec::markAsFailed)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }

        @Test
        @DisplayName("FAILED 상태에서 재전이 시 예외")
        void failedToFailed_throws() {
            Recommendation rec = pendingRec();
            rec.markAsFailed();

            assertThatThrownBy(rec::markAsFailed)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }
    }

    @Nested
    @DisplayName("retryAsPending")
    class RetryAsPending {

        @Test
        @DisplayName("FAILED → PENDING 전이 성공")
        void failedToPending() {
            Recommendation rec = pendingRec();
            rec.markAsFailed();
            rec.retryAsPending();
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.PENDING);
        }

        @Test
        @DisplayName("PENDING 상태에서 retryAsPending 시 예외")
        void pendingToRetry_throws() {
            Recommendation rec = pendingRec();

            assertThatThrownBy(rec::retryAsPending)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FAILED 상태에서만");
        }

        @Test
        @DisplayName("SUCCESS 상태에서 retryAsPending 시 예외")
        void successToRetry_throws() {
            Recommendation rec = pendingRec();
            rec.markAsSuccess();

            assertThatThrownBy(rec::retryAsPending)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FAILED 상태에서만");
        }
    }

    @Nested
    @DisplayName("전체 전이 흐름")
    class FullTransitionFlow {

        @Test
        @DisplayName("PENDING → FAILED → PENDING → SUCCESS 전이 성공")
        void fullRetryFlow() {
            Recommendation rec = pendingRec();
            rec.markAsFailed();
            rec.retryAsPending();
            rec.markAsSuccess();
            assertThat(rec.getStatus()).isEqualTo(RecommendationStatus.SUCCESS);
        }
    }
}
