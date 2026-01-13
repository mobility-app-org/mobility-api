package com.mobility.api.domain.office.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.*;
import com.mobility.api.domain.transporter.entity.Transporter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "배차 상세 응답")
public record GetDispatchDetailRes(
        @Schema(description = "배차 ID", example = "1")
        Long id,

        @Schema(description = "배차 번호", example = "2024-0001")
        String dispatchNumber,

        @Schema(description = "배차 상태", example = "OPEN")
        StatusType status,

        @Schema(description = "요금 (원)", example = "150000")
        Integer charge,

        @Schema(description = "출발지 주소", example = "서울특별시 강남구 테헤란로 123")
        String startLocation,

        @Schema(description = "출발지 위도", example = "37.5065")
        double startLatitude,

        @Schema(description = "출발지 경도", example = "127.0536")
        double startLongitude,

        @Schema(description = "도착지 주소", example = "부산광역시 해운대구 우동 456")
        String destinationLocation,

        @Schema(description = "도착지 위도", example = "35.1595")
        double destinationLatitude,

        @Schema(description = "도착지 경도", example = "129.1603")
        double destinationLongitude,

        @Schema(description = "고객 전화번호 (마스킹 처리)", example = "010-****-5678")
        String clientPhoneNumber,

        @Schema(description = "메모", example = "현관 비밀번호 1234")
        String memo,

        @Schema(description = "콜 타입 (INTERNAL: 자사콜, INTEGRATED: 통합콜)", example = "INTERNAL")
        CallType call,

        @Schema(description = "서비스 타입 (DELIVERY: 탁송, DRIVER: 대리)", example = "DELIVERY")
        ServiceType service,

        @Schema(description = "결제 방식 (CASH: 현금, POSTPAID: 후불, COMPLETE_POSTPAID: 완후)", example = "CASH")
        PaymentType paymentMethod,

        @Schema(description = "톨비 방식 (TOLLGATE_INCLUDED: 톨포, TOLLGATE_SEPARATE: 톨별, HIPASS: 하이패스)", example = "HIPASS")
        TollType tollType,

        @Schema(description = "배차된 기사 ID (미배차 시 null)", example = "1")
        Long transporterId,

        @Schema(description = "배차된 기사 이름 (미배차 시 null)", example = "홍길동")
        String transporterName,

        @Schema(description = "배차된 기사 전화번호 (미배차 시 null)", example = "010-1234-5678")
        String transporterPhone,

        @Schema(description = "사무실 ID", example = "1")
        Long officeId,

        @Schema(description = "생성 일시", example = "2024-01-15T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시", example = "2024-01-15T10:00:00")
        LocalDateTime updatedAt,

        @Schema(description = "배차 할당 일시 (미배차 시 null)", example = "2024-01-15T10:30:00")
        LocalDateTime assignedAt,

        @Schema(description = "완료 일시 (미완료 시 null)", example = "2024-01-15T12:00:00")
        LocalDateTime completedAt,

        @Schema(description = "취소 일시 (미취소 시 null)", example = "2024-01-15T11:00:00")
        LocalDateTime canceledAt,

        @Schema(description = "취소 사유 (최대 200자, 한글/영어 동일)", example = "고객 요청으로 취소", maxLength = 200)
        String cancelReason
) {
    public static GetDispatchDetailRes from(Dispatch dispatch) {
        Transporter transporter = dispatch.getTransporter();

        return new GetDispatchDetailRes(
                dispatch.getId(),
                dispatch.getDispatchNumber(),
                dispatch.getStatus(),
                dispatch.getCharge(),
                dispatch.getStartLocation(),
                dispatch.getStartLatitude(),
                dispatch.getStartLongitude(),
                dispatch.getDestinationLocation(),
                dispatch.getDestinationLatitude(),
                dispatch.getDestinationLongitude(),
                maskPhoneNumber(dispatch.getClientPhoneNumber()),
                dispatch.getMemo(),
                dispatch.getCall(),
                dispatch.getService(),
                dispatch.getPaymentType(),
                dispatch.getTollType(),
                transporter != null ? transporter.getId() : null,
                transporter != null ? transporter.getName() : null,
                transporter != null ? transporter.getPhone() : null,
                dispatch.getOfficeId(),
                dispatch.getCreatedAt(),
                dispatch.getUpdatedAt(),
                dispatch.getAssignedAt(),
                dispatch.getCompletedAt(),
                dispatch.getCanceledAt(),
                dispatch.getCancelReason()
        );
    }

    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }
        // 010-1234-5678 -> 010-****-5678
        // 01012345678 -> 010****5678
        if (phoneNumber.contains("-")) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        // 하이픈 없는 경우 (01012345678)
        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        }
        return phoneNumber;
    }
}
