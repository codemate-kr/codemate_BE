package com.ryu.studyhelper.problem;

import com.ryu.studyhelper.common.dto.ApiResponse;
import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.problem.dto.ProblemRecommendRequest;
import com.ryu.studyhelper.problem.dto.TeamProblemRecommendResponse;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/problem")
@Slf4j
public class ProblemController {

    private final ProblemService problemService;

    @Operation(
            summary = "문제 추천 API",
            description = """
                    주어진 Solved.ac 핸들에 기반해 추천 문제 목록을 반환합니다.  
                    추천 개수(`count`)를 지정하지 않으면 기본값은 3개입니다.
                    """
    )
    @GetMapping("/recommend")
    public ResponseEntity<List<ProblemInfo>> recommend(
            @Parameter(description = "Solved.ac 핸들", example = "ryu_handle")
            @RequestParam String handle,

            @Parameter(description = "추천 문제 개수 (기본값 3)", example = "5")
            @RequestParam(defaultValue = "3") int count,
            
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Member currentUser = principalDetails.getMember();
        log.info("User {} requesting problem recommendations for handle: {}", currentUser.getId(), handle);
        
        List<ProblemInfo> problems = problemService.recommend(handle, count);
        return ResponseEntity.ok(problems);
    }



    @Operation(
            summary = "문제 추천 API (여러 유저)",
            description = """
                    여러 Solved.ac 핸들을 기반으로 추천 문제 목록을 반환합니다.
                    count가 null이면 기본값은 1개입니다.
                    """
    )
    @PostMapping("/recommend")
    public ResponseEntity<List<ProblemInfo>> recommendMulti(
            @Valid @RequestBody ProblemRecommendRequest req,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        
        Member currentUser = principalDetails.getMember();
        log.info("User {} requesting multi-user problem recommendations for handles: {}", 
                currentUser.getId(), req.handles());
        
        int count = (req.count() != null) ? req.count() : 1; // 기본값 1
        List<ProblemInfo> problems = problemService.recommend(req, count);
        return ResponseEntity.ok(problems);
    }


    @Operation(
            summary = "문제 추천 API (팀원 전체)",
            description = """
                    특정 팀의 Solved.ac 핸들을 기반으로 추천 문제 목록을 반환합니다.
                    count가 null이면 기본값은 1개입니다.
                    """
    )
    @PostMapping("/recommend-team")
//    @PreAuthorize("authentication.principal.member.verified == true")
    public ResponseEntity<ApiResponse<TeamProblemRecommendResponse>> recommendTeam(
            @Parameter(description = "팀 ID", example = "1")
            @RequestParam Long teamId,

            @Parameter(description = "추천 문제 개수 (기본값 1)", example = "1")
            @RequestParam(defaultValue = "1") int count,
            
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        Member currentUser = principalDetails.getMember();
        log.info("User {} requesting team problem recommendations for team: {}", 
                currentUser.getId(), teamId);
        
        TeamProblemRecommendResponse response = problemService.recommendTeam(teamId, count);
        return ResponseEntity.ok(ApiResponse.createSuccess(response, CustomResponseStatus.SUCCESS));
    }
}   