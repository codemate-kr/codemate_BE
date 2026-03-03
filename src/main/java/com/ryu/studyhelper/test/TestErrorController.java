package com.ryu.studyhelper.test;

import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentry 테스트용 에러 발생 엔드포인트
 *
 * 주의: 이 컨트롤러는 local 및 test 환경에서만 활성화됩니다.
 * 운영 환경(prod)에서는 자동으로 비활성화됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@Profile({"local", "test"})
@Tag(name = "Test", description = "테스트용 API (local/test 환경에서만 활성화)")
public class TestErrorController {

    @GetMapping("/error/runtime")
    @Operation(summary = "RuntimeException 발생 테스트", description = "예상하지 못한 서버 에러를 시뮬레이션합니다. Sentry에 ERROR 레벨로 전송됩니다.")
    public String throwRuntimeException() {
        log.info("RuntimeException 테스트 엔드포인트 호출");
        throw new RuntimeException("테스트: RuntimeException 발생 - Sentry 에러 추적 확인");
    }

    @GetMapping("/error/custom")
    @Operation(summary = "CustomException 발생 테스트 (서버 에러)", description = "서버 에러(6xxx) CustomException을 시뮬레이션합니다. Sentry로 전송됩니다.")
    public String throwCustomException() {
        log.info("CustomException 테스트 엔드포인트 호출");
        throw new CustomException(CustomResponseStatus.SOLVED_AC_API_ERROR);
    }

    @GetMapping("/error/custom404")
    @Operation(summary = "CustomException 발생 테스트 (서버 에러)", description = "서버 에러(4xxx)")
    public String throwCustomException404() {
        log.info("CustomException 테스트 엔드포인트 호출");
        throw new CustomException(CustomResponseStatus.BADGE_NOT_FOUND);
    }

    @GetMapping("/error/npe")
    @Operation(summary = "NullPointerException 발생 테스트", description = "NPE를 시뮬레이션합니다. Sentry에 ERROR 레벨로 전송됩니다.")
    public String throwNullPointerException() {
        log.info("NullPointerException 테스트 엔드포인트 호출");
        String nullString = null;
        return nullString.toUpperCase(); // NPE 발생
    }

    @GetMapping("/error/array-index")
    @Operation(summary = "ArrayIndexOutOfBoundsException 발생 테스트", description = "배열 인덱스 에러를 시뮬레이션합니다.")
    public String throwArrayIndexOutOfBoundsException() {
        log.info("ArrayIndexOutOfBoundsException 테스트 엔드포인트 호출");
        int[] array = new int[5];
        return "Value: " + array[10]; // ArrayIndexOutOfBoundsException 발생
    }

    @GetMapping("/error/division-by-zero")
    @Operation(summary = "ArithmeticException 발생 테스트", description = "0으로 나누기 에러를 시뮬레이션합니다.")
    public String throwArithmeticException() {
        log.info("ArithmeticException 테스트 엔드포인트 호출");
        int result = 100 / 0; // ArithmeticException 발생
        return "Result: " + result;
    }

    @GetMapping("/error/with-message")
    @Operation(summary = "커스텀 메시지 에러 테스트", description = "커스텀 메시지를 포함한 에러를 발생시킵니다.")
    public String throwErrorWithMessage(@RequestParam(defaultValue = "테스트 에러 메시지") String message) {
        log.info("커스텀 메시지 에러 테스트: {}", message);
        throw new RuntimeException(message);
    }

    @GetMapping("/success")
    @Operation(summary = "정상 응답 테스트", description = "에러 없이 정상적으로 응답합니다.")
    public String successEndpoint() {
        log.info("정상 응답 테스트 엔드포인트 호출");
        return "테스트 성공: 에러가 발생하지 않았습니다.";
    }
}