package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "완료된 배차 목록 항목 응답")
public record CompletedDispatchListItemRes(
        @Schema(description = "배차 ID", example = "1")
        Long id,

        @Schema(description = "배차 번호", example = "2025-0001")
        String dispatchNumber,

        @Schema(description = "서비스 타입 (DELIVERY: 탁송, DRIVER: 대리)", example = "DELIVERY")
        String serviceType,

        @Schema(description = "배차 할당 시간", example = "2025-11-22T14:15:00")
        LocalDateTime assignedAt,

        @Schema(description = "배차 완료 시간", example = "2025-11-22T15:19:00")
        LocalDateTime completedAt,

        @Schema(description = "출발지", example = "부안상서면부장1길 23")
        String startLocation,

        @Schema(description = "목적지", example = "수원평동, 임광모터스")
        String destinationLocation,

        @Schema(description = "요금", example = "110000")
        Integer charge,

        @Schema(description = "사무실 이름", example = "(주)대리GO")
        String officeName,

        @Schema(description = "사무실 전화번호", example = "02-1234-5678")
        String officeTelNumber
) {
    public static CompletedDispatchListItemRes from(Dispatch dispatch, String officeName, String officeTelNumber) {
        return CompletedDispatchListItemRes.builder()
                .id(dispatch.getId())
                .dispatchNumber(dispatch.getDispatchNumber())
                .serviceType(dispatch.getService() != null ? dispatch.getService().name() : null)
                .assignedAt(dispatch.getAssignedAt())
                .completedAt(dispatch.getCompletedAt())
                .startLocation(dispatch.getStartLocation())
                .destinationLocation(dispatch.getDestinationLocation())
                .charge(dispatch.getCharge())
                .officeName(officeName)
                .officeTelNumber(officeTelNumber)
                .build();
    }
}
