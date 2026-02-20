package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.PaymentType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.TollType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Schema(description = "완료된 배차 상세 정보 응답")
public record CompletedDispatchDetailRes(
        @Schema(description = "배차 ID", example = "1")
        Long id,

        @Schema(description = "사무실 이름", example = "태) (주)대리GO")
        String officeName,

        @Schema(description = "사무실 전화번호", example = "16887141")
        String officeTelNumber,

        @Schema(description = "출발지", example = "부안상서면부장1길 23")
        String startLocation,

        @Schema(description = "목적지", example = "수원평동, 임광모터스")
        String destinationLocation,

        @Schema(description = "요금", example = "110000")
        Integer charge,

        @Schema(description = "서비스 타입 (DELIVERY: 탁송, DRIVER: 대리)", example = "DELIVERY")
        String serviceType,

        @Schema(description = "배차 번호", example = "2025-0001")
        String dispatchNumber,

        @Schema(description = "배차 생성 시간", example = "2025-11-22T11:43:00")
        LocalDateTime createdAt,

        @Schema(description = "배차 할당 시간", example = "2025-11-22T11:48:00")
        LocalDateTime assignedAt,

        @Schema(description = "배차 완료 시간", example = "2025-11-22T15:19:00")
        LocalDateTime completedAt,

        @Schema(description = "차량 타입", example = "null", nullable = true)
        String carType,

        @Schema(description = "차량 번호", example = "null", nullable = true)
        String carNumber,

        @Schema(description = "태그 목록 (결제 방식, 톨게이트 방식 등)", example = "[\"현금\", \"톨포\"]")
        List<String> tags
) {
    public static CompletedDispatchDetailRes from(Dispatch dispatch, String officeName, String officeTelNumber) {
        return CompletedDispatchDetailRes.builder()
                .id(dispatch.getId())
                .officeName(officeName)
                .officeTelNumber(officeTelNumber)
                .startLocation(dispatch.getStartLocation())
                .destinationLocation(dispatch.getDestinationLocation())
                .charge(dispatch.getCharge())
                .serviceType(dispatch.getService() != null ? dispatch.getService().name() : null)
                .dispatchNumber(dispatch.getDispatchNumber())
                .createdAt(dispatch.getCreatedAt())
                .assignedAt(dispatch.getAssignedAt())
                .completedAt(dispatch.getCompletedAt())
                .carType(null) // TODO: 차량 타입 필드 추가 시 매핑
                .carNumber(null) // TODO: 차량 번호 필드 추가 시 매핑
                .tags(buildTags(dispatch))
                .build();
    }

    /**
     * 배차 정보로부터 태그 목록 생성
     * - 결제 방식 (현금, 후불, 완후)
     * - 톨게이트 방식 (톨포, 톨별, 하이패스)
     */
    private static List<String> buildTags(Dispatch dispatch) {
        List<String> tags = new ArrayList<>();

        // 결제 방식 태그
        if (dispatch.getPaymentType() != null) {
            tags.add(getPaymentTypeLabel(dispatch.getPaymentType()));
        }

        // 톨게이트 방식 태그
        if (dispatch.getTollType() != null) {
            tags.add(getTollTypeLabel(dispatch.getTollType()));
        }

        return tags;
    }

    /**
     * PaymentType enum을 한글 라벨로 변환
     */
    private static String getPaymentTypeLabel(PaymentType paymentType) {
        return switch (paymentType) {
            case CASH -> "현금";
            case POSTPAID -> "후불";
            case COMPLETE_POSTPAID -> "완후";
        };
    }

    /**
     * TollType enum을 한글 라벨로 변환
     */
    private static String getTollTypeLabel(TollType tollType) {
        return switch (tollType) {
            case TOLLGATE_INCLUDED -> "톨포";
            case TOLLGATE_SEPARATE -> "톨별";
            case HIPASS -> "하이패스";
        };
    }
}
