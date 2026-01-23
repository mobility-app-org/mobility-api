package com.mobility.api.domain.office.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "배차 피드 응답 DTO")
@Builder
public record DispatchFeedRes(
        @Schema(description = "피드 ID", example = "open-123")
        String id,

        @Schema(description = "피드 타입", example = "assigned", allowableValues = {"open", "assigned", "completed", "canceled"})
        String type,

        @Schema(description = "배차 ID", example = "123")
        Long dispatchId,

        @Schema(description = "배차 번호", example = "2024-0001")
        String dispatchNumber,

        @Schema(description = "기사 이름 (assigned, completed 시에만 존재)", example = "김철수")
        String transporterName,

        @Schema(description = "피드 메시지", example = "김철수 기사가 콜 #2024-0001을 배차 받았습니다")
        String message,

        @Schema(description = "이벤트 발생 시간 (BaseEntity.createdAt 또는 이벤트별 타임스탬프)", example = "2024-01-15T10:32:00")
        LocalDateTime timestamp
) {
}