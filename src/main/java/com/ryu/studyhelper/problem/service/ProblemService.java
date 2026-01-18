package com.ryu.studyhelper.problem.service;

import com.ryu.studyhelper.common.enums.CustomResponseStatus;
import com.ryu.studyhelper.common.exception.CustomException;
import com.ryu.studyhelper.infrastructure.mail.MailSendService;
import com.ryu.studyhelper.problem.dto.ProblemRecommendRequest;
import com.ryu.studyhelper.problem.dto.TeamProblemRecommendResponse;
import com.ryu.studyhelper.solvedac.SolvedAcService;
import com.ryu.studyhelper.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.team.repository.TeamMemberRepository;
import com.ryu.studyhelper.team.repository.TeamRepository;
import com.ryu.studyhelper.infrastructure.mail.dto.MailTxtSendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class ProblemService {
    private final SolvedAcService solvedAcService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MailSendService mailSendService;

    /**
     * 핸들 목록을 기반으로 문제 추천
     */
    public List<ProblemInfo> recommend(List<String> handles, int count) {
        return solvedAcService.recommendUnsolvedProblems(handles, count);
    }

    /**
     * 핸들 목록과 난이도 범위를 기반으로 문제 추천
     */
    public List<ProblemInfo> recommend(List<String> handles, int count, Integer minLevel, Integer maxLevel) {
        return solvedAcService.recommendUnsolvedProblems(handles, count, minLevel, maxLevel);
    }

    /**
     * 핸들 목록, 난이도 범위, 태그 필터를 기반으로 문제 추천
     */
    public List<ProblemInfo> recommend(List<String> handles, int count, Integer minLevel, Integer maxLevel, List<String> tagKeys) {
        return solvedAcService.recommendUnsolvedProblems(handles, count, minLevel, maxLevel, tagKeys);
    }



    public List<ProblemInfo> recommend(ProblemRecommendRequest request, int count) {
        return recommend(request.handles(), count);
    }
    public List<ProblemInfo> recommend(String handle , int count) {
        return recommend(List.of(handle), count);
    }

    // 팀원 전체 문제 추천
    public TeamProblemRecommendResponse recommendTeam(Long teamId, int count) {
        // 팀의 Solved.ac 핸들 목록을 가져옴
        List<String> handles = teamMemberRepository.findHandlesByTeamId(teamId);
        if(handles.isEmpty()) {
            throw new CustomException(CustomResponseStatus.TEAM_NOT_FOUND);
        }

        // 팀의 문제 추천
        List<ProblemInfo> problems = recommend(handles, count);

        // 이메일 전송: 팀 멤버 전원에게 추천 목록 발송 (텍스트 메일)
        List<String> emails = teamMemberRepository.findEmailsByTeamId(teamId);

        if (emails.isEmpty()) throw new CustomException(CustomResponseStatus.TEAM_NOT_FOUND);


//        String subject = "[StudyHalp!] " + teamId + " problem recommendations (" + problems.size() + ")";

        String content = problems.stream()
                .map(p -> "%s : %s\n(%s)\n\n".formatted(
                        p.problemId(),
                        p.titleKo(),
                        p.url()
                ))
                .collect(Collectors.joining());

        for (int i=0; i<emails.size(); i++) {
//            String subject = "[studyHalp] %s님, 오늘의 추천문제입니다!".formatted(handles.get(i));
            String subject = "%s님, 오늘의 추천문제입니다!".formatted(handles.get(i));
            mailSendService.sendTxtEmail(new MailTxtSendDto(emails.get(i), subject, content));
            System.out.println("메일 전송 완료: " + emails.get(i));
        }


        // 팀 문제 추천 응답 생성

        return TeamProblemRecommendResponse.from(problems, handles);
    }


}