package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
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
 * 배차 상세 조회 응답 DTO
 */
@Builder
@Schema(description = "배차 상세 조회 응답")
public record DispatchDetailRes(
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

        @Schema(description = "현재 로그인한 기사와 출발지 간 직선거리 (km). 기사 위치가 없으면 null", example = "11.5", nullable = true)
        Double distanceKm,

        @Schema(description = "배차 태그 (경유 여부, 결제 방식, 톨비 방식)", example = "[\"경유\", \"현금\", \"톨게이트 포함\"]")
        List<String> tags
) {
    /**
     * Entity -> DTO 변환 메서드
     * @param dispatch 배차 엔티티
     * @param distanceKm 출발지와 기사의 직선거리 (km), null 가능
     * @return DispatchDetailRes
     */
    public static DispatchDetailRes from(Dispatch dispatch, Double distanceKm) {
        List<String> tags = buildTags(dispatch);

        return DispatchDetailRes.builder()
                .id(dispatch.getId())
                .serviceType(dispatch.getService())
                .charge(dispatch.getCharge())
                .startLocation(dispatch.getStartLocation())
                .destinationLocation(dispatch.getDestinationLocation())
                .status(dispatch.getStatus())
                .distanceKm(distanceKm)
                .tags(tags)
                .build();
    }

    /**
     * Tag 리스트 생성 메서드
     * 경유 여부, 결제 방식, 톨비 방식을 String 리스트로 변환
     */
    private static List<String> buildTags(Dispatch dispatch) {
        List<String> tags = new ArrayList<>();

        // 경유 여부 (VIA가 있으면 "경유" 추가)
        if (dispatch.getViaType() == ViaType.VIA) {
            tags.add("경유");
        }

        // 결제 방식
        if (dispatch.getPaymentType() != null) {
            switch (dispatch.getPaymentType()) {
                case CASH -> tags.add("현금");
                case POSTPAID -> tags.add("후불");
                case COMPLETE_POSTPAID -> tags.add("완후");
            }
        }

        // 톨비 방식
        if (dispatch.getTollType() != null) {
            switch (dispatch.getTollType()) {
                case TOLLGATE_INCLUDED -> tags.add("톨게이트 포함");
                case TOLLGATE_SEPARATE -> tags.add("톨게이트 별도");
                case HIPASS -> tags.add("하이패스");
            }
        }

        return tags;
    }
}