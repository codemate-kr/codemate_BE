package com.ryu.studyhelper;

import com.ryu.studyhelper.dto.ProblemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class TestController {
    private final TestService testService;

    @GetMapping("/test/{handle}")
    public void test(@PathVariable String handle) {
        testService.printUserInfo(handle);
    }

    @PostMapping("/recommend")
    public List<ProblemInfo> recommend(@RequestBody List<String> handles) {
        return testService.recommend(handles, 1); // 기본 추천 개수 3
    }
}
