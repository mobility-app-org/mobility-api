package com.mobility.api.domain.office.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(description = "대시보드 배차 피드 응답 DTO - 최근 배차 이벤트 정보")
@Builder
public record DispatchFeedRes(
        @Schema(
                description = "피드 고유 ID (연번 형식: feed-01, feed-02, ...)",
                example = "feed-01",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String id,

        @Schema(
                description = """
                        피드 타입
                        - open: 배차 등록
                        - assigned: 배차 할당 (기사 배정)
                        - completed: 운송 완료
                        - canceled: 배차 취소
                        """,
                example = "assigned",
                allowableValues = {"open", "assigned", "completed", "canceled"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String type,

        @Schema(
                description = "배차 ID (Dispatch 테이블의 PK)",
                example = "123",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long dispatchId,

        @Schema(
                description = "배차 번호 (예: 2024-0001) - null일 수 있음",
                example = "2024-0001",
                nullable = true
        )
        String dispatchNumber,

        @Schema(
                description = "기사 이름 - assigned, completed 타입일 때만 값이 있고, open, canceled 타입일 때는 null",
                example = "김철수",
                nullable = true
        )
        String transporterName,

        @Schema(
                description = """
                        자동 생성된 피드 메시지
                        - open: "배차 #{번호}가 등록되었습니다"
                        - assigned: "{기사명} 기사가 콜 #{번호}을 배차 받았습니다"
                        - completed: "{기사명} 기사가 콜 #{번호}을 완료했습니다"
                        - canceled: "배차 #{번호}이 취소되었습니다"
                        """,
                example = "김철수 기사가 콜 #2024-0001을 배차 받았습니다",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message,

        @Schema(
                description = """
                        이벤트 발생 시간 (타입별 타임스탬프)
                        - open: createdAt (배차 생성 시간)
                        - assigned: assignedAt (배차 할당 시간)
                        - completed: completedAt (운송 완료 시간)
                        - canceled: canceledAt (취소 시간)
                        """,
                example = "2024-01-15T10:32:00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDateTime timestamp
) {
}