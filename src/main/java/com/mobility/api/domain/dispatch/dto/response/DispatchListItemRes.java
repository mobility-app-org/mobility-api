package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.dto.DispatchDistanceProjection;
import com.mobility.api.domain.dispatch.enums.PaymentType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.dispatch.enums.TollType;
import com.mobility.api.domain.dispatch.enums.ViaType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * 기사용 배차 리스트 조회 응답 DTO
 * Projection의 미터 값을 받아 km로 변환하여 전달
 */
@Builder
@Schema(description = "배차 리스트 아이템 (거리 포함)")
public record DispatchListItemRes(
        @Schema(description = "배차 ID", example = "1")
        Long id,

        @Schema(description = "서비스 타입 (DELIVERY: 탁송, DRIVER: 대리)", example = "DELIVERY")
        ServiceType serviceType,

        @Schema(description = "요금 (원)", example = "50000")
        Integer charge,

        @Schema(description = "출발지", example = "강남역")
        String startLocation,

        @Schema(description = "도착지", example = "판교역")
        String destinationLocation,

        @Schema(description = "배차 상태 (OPEN: 대기, ASSIGNED: 배정, COMPLETED: 완료, CANCELED: 취소)", example = "OPEN")
        StatusType status,

        @Schema(description = "현재 기사와 출발지 간 직선거리 (km)", example = "11.5")
        Double distanceKm,

        @Schema(description = "배차 태그 (경유 여부, 결제 방식, 톨비 방식)", example = "[\"경유\", \"현금\", \"톨게이트 포함\"]")
        List<String> tags
) {
    /**
     * Projection -> DTO 변환 메서드
     * @param proj Native Query 결과 Projection
     * @return DispatchListItemRes
     */
    public static DispatchListItemRes from(DispatchDistanceProjection proj) {
        // 미터(m) -> 킬로미터(km) 변환
        double meters = proj.getDistanceInMeters() != null ? proj.getDistanceInMeters() : 0.0;
        double km = Math.round((meters / 1000.0) * 100.0) / 100.0;

        // ServiceType enum 변환
        ServiceType serviceType = proj.getServiceType() != null
            ? ServiceType.valueOf(proj.getServiceType())
            : null;

        // StatusType enum 변환
        StatusType status = proj.getStatus() != null
            ? StatusType.valueOf(proj.getStatus())
            : null;

        // ViaType enum 변환
        ViaType viaType = proj.getViaType() != null
            ? ViaType.valueOf(proj.getViaType())
            : null;

        // PaymentType enum 변환
        PaymentType paymentType = proj.getPaymentType() != null
            ? PaymentType.valueOf(proj.getPaymentType())
            : null;

        // TollType enum 변환
        TollType tollType = proj.getTollType() != null
            ? TollType.valueOf(proj.getTollType())
            : null;

        // 태그 리스트 생성
        List<String> tags = buildTags(viaType, paymentType, tollType);

        return DispatchListItemRes.builder()
                .id(proj.getId())
                .serviceType(serviceType)
                .charge(proj.getCharge())
                .startLocation(proj.getStartLocation())
                .destinationLocation(proj.getDestinationLocation())
                .status(status)
                .distanceKm(km)
                .tags(tags)
                .build();
    }

    /**
     * Tag 리스트 생성 메서드
     * 경유 여부, 결제 방식, 톨비 방식을 String 리스트로 변환
     */
    private static List<String> buildTags(ViaType viaType, PaymentType paymentType, TollType tollType) {
        List<String> tags = new ArrayList<>();

        // 경유 여부 (VIA가 있으면 "경유" 추가)
        if (viaType == ViaType.VIA) {
            tags.add("경유");
        }

        // 결제 방식
        if (paymentType != null) {
            switch (paymentType) {
                case CASH -> tags.add("현금");
                case POSTPAID -> tags.add("후불");
                case COMPLETE_POSTPAID -> tags.add("완후");
            }
        }

        // 톨비 방식
        if (tollType != null) {
            switch (tollType) {
                case TOLLGATE_INCLUDED -> tags.add("톨게이트 포함");
                case TOLLGATE_SEPARATE -> tags.add("톨게이트 별도");
                case HIPASS -> tags.add("하이패스");
            }
        }

        return tags;
    }
}