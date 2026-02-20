package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.PaymentType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.dispatch.enums.TollType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 현재 배차중인 오더 상세 정보 조회 응답 DTO
 */
@Builder
@Schema(description = "현재 배차중인 오더 상세 정보 응답")
public record CurrentDispatchDetailRes(
        @Schema(description = "배차 ID", example = "1")
        Long id,

        @Schema(description = "배차 상태", example = "ASSIGNED")
        StatusType status,

        @Schema(description = "요금 (원)", example = "150000")
        Integer charge,

        @Schema(description = "출발지", example = "서울특별시 강남구 테헤란로 123")
        String startLocation,

        @Schema(description = "도착지", example = "부산광역시 해운대구 우동 456")
        String destinationLocation,

        @Schema(description = "고객 전화번호", example = "010-****-5678")
        String clientPhoneNumber,

        @Schema(description = "메모", example = "학교 비밀번호 1234")
        String memo,

        @Schema(description = "콜 타입 (INTERNAL: 자사콜, INTEGRATED: 통합콜)", example = "INTERNAL")
        CallType call,

        @Schema(description = "서비스 타입 (DELIVERY: 탁송, DRIVER: 대리)", example = "DELIVERY")
        ServiceType service,

        @Schema(description = "결제 방식 (CASH: 현금, POSTPAID: 후불, COMPLETE_POSTPAID: 완후)", example = "CASH")
        PaymentType paymentMethod,

        @Schema(description = "톨비 방식 (TOLLGATE_INCLUDED: 톨게이트 포함, TOLLGATE_SEPARATE: 톨게이트 별도, HIPASS: 하이패스)", example = "HIPASS")
        TollType tollType,

        @Schema(description = "사무실 ID", example = "1")
        Long officeId,

        @Schema(description = "사무실 전화번호", example = "02-1234-5678")
        String officeTelNumber,

        @Schema(description = "생성일시", example = "2024-01-15T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일시", example = "2024-01-15T10:00:00")
        LocalDateTime updatedAt,

        @Schema(description = "배차 할당 시간", example = "2024-01-15T10:05:00")
        LocalDateTime assignedAt
) {
    /**
     * Entity -> DTO 변환 메서드
     * @param dispatch 배차 엔티티
     * @param officeTelNumber 사무실 전화번호
     * @return CurrentDispatchDetailRes
     */
    public static CurrentDispatchDetailRes from(Dispatch dispatch, String officeTelNumber) {
        return CurrentDispatchDetailRes.builder()
                .id(dispatch.getId())
                .status(dispatch.getStatus())
                .charge(dispatch.getCharge())
                .startLocation(dispatch.getStartLocation())
                .destinationLocation(dispatch.getDestinationLocation())
                .clientPhoneNumber(dispatch.getClientPhoneNumber())
                .memo(dispatch.getMemo())
                .call(dispatch.getCall())
                .service(dispatch.getService())
                .paymentMethod(dispatch.getPaymentType())
                .tollType(dispatch.getTollType())
                .officeId(dispatch.getOfficeId())
                .officeTelNumber(officeTelNumber)
                .createdAt(dispatch.getCreatedAt())
                .updatedAt(dispatch.getUpdatedAt())
                .assignedAt(dispatch.getAssignedAt())
                .build();
    }
}
