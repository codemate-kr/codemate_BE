package com.ryu.studyhelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class CiFailureTest {

    @Test
    void 의도적_실패_테스트() {
        fail("CI 테스트 실패 리포트 확인용");
    }
}