package com.ryu.studyhelper;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class TestController {
    private final TestService testService;

    @GetMapping("/test/{handle}")
    public void test(@PathVariable String handle) {
        testService.getUnsolvedProblems(handle);
    }

}
