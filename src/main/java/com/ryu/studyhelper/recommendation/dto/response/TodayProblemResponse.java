package com.ryu.studyhelper.recommendation.dto.response;

import com.ryu.studyhelper.common.util.ProblemUrlUtils;
import com.ryu.studyhelper.problem.dto.projection.ProblemTagProjection;
import com.ryu.studyhelper.recommendation.domain.Recommendation;
import com.ryu.studyhelper.recommendation.dto.projection.ProblemWithSolvedStatusProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 오늘의 문제 조회용 응답 DTO (해결 여부 + 태그 포함)
 */
public record TodayProblemResponse(
        Long recommendationId,
        LocalDateTime createdAt,
        List<ProblemWithSolvedStatus> problems
) {
    /**
     * 태그 정보
     */
    public record TagInfo(
            String key,
            String nameKo,
            String nameEn
    ) {
        public static TagInfo from(ProblemTagProjection projection) {
            return new TagInfo(
                    projection.getTagKey(),
                    projection.getNameKo(),
                    projection.getNameEn()
            );
        }
    }

    public record ProblemWithSolvedStatus(
            Long problemId,
            String title,
            String titleKo,
            Integer level,
            String url,
            Integer acceptedUserCount,
            Double averageTries,
            Boolean isSolved,
            List<TagInfo> tags
    ) {
        public static ProblemWithSolvedStatus from(
                ProblemWithSolvedStatusProjection projection,
                List<TagInfo> tags
        ) {
            return new ProblemWithSolvedStatus(
                    projection.getProblemId(),
                    projection.getTitle(),
                    projection.getTitleKo(),
                    projection.getLevel(),
                    ProblemUrlUtils.generateProblemUrl(projection.getProblemId()),
                    projection.getAcceptedUserCount(),
                    projection.getAverageTries(),
                    projection.getIsSolved(),
                    tags
            );
        }
    }

    /**
     * 프로젝션과 태그 정보를 병합하여 응답 생성
     * @param recommendation 추천 엔티티
     * @param projections 문제 + 해결 상태 프로젝션
     * @param tagProjections 문제별 태그 프로젝션
     */
    public static TodayProblemResponse from(
            Recommendation recommendation,
            List<ProblemWithSolvedStatusProjection> projections,
            List<ProblemTagProjection> tagProjections
    ) {
        // 문제 ID별 태그 목록 그룹화
        Map<Long, List<TagInfo>> tagsByProblemId = tagProjections.stream()
                .collect(Collectors.groupingBy(
                        ProblemTagProjection::getProblemId,
                        Collectors.mapping(TagInfo::from, Collectors.toList())
                ));

        List<ProblemWithSolvedStatus> problems = projections.stream()
                .map(projection -> ProblemWithSolvedStatus.from(
                        projection,
                        tagsByProblemId.getOrDefault(projection.getProblemId(), List.of())
                ))
                .toList();

        return new TodayProblemResponse(recommendation.getId(), recommendation.getCreatedAt(), problems);
    }
}