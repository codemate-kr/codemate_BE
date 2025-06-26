package com.ryu.studyhelper.solvedac;

import com.ryu.studyhelper.dto.ProblemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class TestController {
    private final SolvedAcService solvedAcService;

    @GetMapping("/test/{handle}")
    public void test(@PathVariable String handle) {
        solvedAcService.getUserInfo(handle);
    }

    @PostMapping("/recommend")
    public List<ProblemInfo> recommend(@RequestBody List<String> handles) {
        return solvedAcService.recommendUnsolvedProblems(handles, 3); // 기본 추천 개수 3
    }
}
