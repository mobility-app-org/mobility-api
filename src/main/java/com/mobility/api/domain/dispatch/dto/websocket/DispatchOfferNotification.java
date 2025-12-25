package com.mobility.api.domain.dispatch.dto.websocket;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.entity.DispatchOffer;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 배차 알림 WebSocket 메시지
 * - 서버 → 기사로 전송되는 배차 제안 알림
 */
@Builder
public record DispatchOfferNotification(
        Long offerId,                       // 제안 ID
        Long dispatchId,                    // 배차 ID
        String startLocation,               // 출발지
        Double startLatitude,               // 출발지 위도
        Double startLongitude,              // 출발지 경도
        String destinationLocation,         // 도착지
        Double destinationLatitude,         // 도착지 위도
        Double destinationLongitude,        // 도착지 경도
        Integer charge,                     // 요금
        ServiceType serviceType,            // 서비스 타입 (DELIVERY/DRIVER)
        Integer sequence,                   // 순서 (1~10)
        Double distanceKm,                  // 거리 (km)
        LocalDateTime offeredAt             // 제안 시각
) {
    /**
     * DispatchOffer 엔티티로부터 DTO 생성
     */
    public static DispatchOfferNotification from(DispatchOffer offer, Double distanceKm) {
        Dispatch dispatch = offer.getDispatch();
        return DispatchOfferNotification.builder()
                .offerId(offer.getId())
                .dispatchId(dispatch.getId())
                .startLocation(dispatch.getStartLocation())
                .startLatitude(dispatch.getStartLatitude())
                .startLongitude(dispatch.getStartLongitude())
                .destinationLocation(dispatch.getDestinationLocation())
                .destinationLatitude(dispatch.getDestinationLatitude())
                .destinationLongitude(dispatch.getDestinationLongitude())
                .charge(dispatch.getCharge())
                .serviceType(dispatch.getService())
                .sequence(offer.getSequence())
                .distanceKm(distanceKm)
                .offeredAt(offer.getOfferedAt())
                .build();
    }
}
