package com.ryu.studyhelper.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "문제 추천 요청 DTO (record)")
public record ProblemRecommendRequest(

        @NotEmpty(message = "handles는 최소 1개 이상이어야 합니다.")
        @Schema(description = "추천을 받을 Solved.ac 핸들 목록", example = "[\"alice\", \"bob\"]")
        List<@NotBlank(message = "handle 값은 비어 있을 수 없습니다.") String> handles,

        // null이면 컨트롤러에서 기본값 1로 처리, 값이 오면 1 이상이어야 함
        @Positive(message = "count가 null이 아니면 1 이상이어야 합니다.")
        @Schema(description = "추천 문제 개수 (null이면 기본값 1)", example = "5", defaultValue = "1")
        Integer count
) {}