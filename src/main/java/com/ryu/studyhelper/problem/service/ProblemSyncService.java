package com.ryu.studyhelper.problem.service;

import com.ryu.studyhelper.problem.repository.ProblemRepository;
import com.ryu.studyhelper.problem.domain.Problem;
import com.ryu.studyhelper.problem.domain.ProblemTag;
import com.ryu.studyhelper.problem.domain.Tag;
import com.ryu.studyhelper.problem.repository.ProblemTagRepository;
import com.ryu.studyhelper.problem.repository.TagRepository;
import com.ryu.studyhelper.infrastructure.solvedac.dto.ProblemInfo;
import com.ryu.studyhelper.infrastructure.solvedac.dto.SolvedAcTagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 문제 메타데이터 동기화 서비스
 * - solved.ac API 응답에서 문제 정보(난이도, 해결수, 평균시도횟수, 태그)를 DB에 upsert
 * - 문제 추천 시 호출되어 문제 정보를 최신 상태로 유지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemSyncService {

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final ProblemTagRepository problemTagRepository;

    /**
     * 여러 문제의 메타데이터를 동기화
     * @param problemInfos solved.ac API 응답 목록
     * @return 동기화된 Problem 엔티티 목록
     */
    @Transactional
    public List<Problem> syncProblems(List<ProblemInfo> problemInfos) {
        List<Problem> syncedProblems = new ArrayList<>();

        for (ProblemInfo info : problemInfos) {
            Problem problem = syncProblem(info);
            syncedProblems.add(problem);
        }

        log.info("문제 메타데이터 동기화 완료: {} 개", syncedProblems.size());
        return syncedProblems;
    }

    /**
     * 단일 문제의 메타데이터를 동기화 (문제 정보 + 태그)
     * @param problemInfo solved.ac API 응답
     * @return 동기화된 Problem 엔티티
     */
    @Transactional
    public Problem syncProblem(ProblemInfo problemInfo) {
        // 1. Problem upsert
        Problem problem = upsertProblem(problemInfo);

        // 2. 태그 동기화
        if (problemInfo.tags() != null && !problemInfo.tags().isEmpty()) {
            syncTagsForProblem(problemInfo.tags(), problem);
        }

        return problem;
    }

    /**
     * Problem upsert (없으면 INSERT, 있으면 UPDATE)
     */
    private Problem upsertProblem(ProblemInfo info) {
        return problemRepository.findById(info.problemId())
                .map(existingProblem -> {
                    existingProblem.updateMetadata(
                            info.titleKo(),
                            info.level(),
                            info.acceptedUserCount(),
                            info.averageTries()
                    );
                    return existingProblem;
                })
                .orElseGet(() -> {
                    Problem newProblem = Problem.create(
                            info.problemId(),
                            info.titleKo(),
                            info.level(),
                            info.acceptedUserCount(),
                            info.averageTries()
                    );
                    return problemRepository.save(newProblem);
                });
    }

    /**
     * 문제의 태그 정보를 동기화
     */
    private void syncTagsForProblem(List<SolvedAcTagInfo> tagInfos, Problem problem) {
        for (SolvedAcTagInfo tagInfo : tagInfos) {
            // 메타 태그는 제외
            if (tagInfo.isMeta()) {
                continue;
            }

            // Tag upsert
            Tag tag = upsertTag(tagInfo);

            // ProblemTag 연결 (중복 방지)
            createProblemTagIfNotExists(problem, tag);
        }
    }

    /**
     * Tag upsert (없으면 INSERT, 있으면 UPDATE)
     */
    private Tag upsertTag(SolvedAcTagInfo tagInfo) {
        return tagRepository.findByKey(tagInfo.key())
                .map(existingTag -> {
                    existingTag.update(tagInfo.getNameKo(), tagInfo.getNameEn());
                    return existingTag;
                })
                .orElseGet(() -> {
                    Tag newTag = Tag.create(
                            tagInfo.key(),
                            tagInfo.getNameKo(),
                            tagInfo.getNameEn()
                    );
                    return tagRepository.save(newTag);
                });
    }

    /**
     * ProblemTag 연결 생성 (이미 존재하면 스킵)
     */
    private void createProblemTagIfNotExists(Problem problem, Tag tag) {
        boolean exists = problemTagRepository.existsByProblemIdAndTagKey(problem.getId(), tag.getKey());
        if (!exists) {
            ProblemTag problemTag = ProblemTag.create(problem, tag);
            problemTagRepository.save(problemTag);
        }
    }
}