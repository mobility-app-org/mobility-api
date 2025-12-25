package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.DispatchOffer;
import com.mobility.api.domain.dispatch.enums.OfferStatus;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 배차 제안 응답 DTO
 * - 배차 제안 이력 조회 시 사용
 */
@Builder
public record DispatchOfferRes(
        Long offerId,               // 제안 ID
        Long dispatchId,            // 배차 ID
        Long transporterId,         // 기사 ID
        String transporterName,     // 기사 이름
        OfferStatus status,         // 제안 상태
        Integer sequence,           // 순서
        LocalDateTime offeredAt,    // 제안 시각
        LocalDateTime respondedAt   // 응답 시각
) {
    /**
     * DispatchOffer 엔티티로부터 DTO 생성
     */
    public static DispatchOfferRes from(DispatchOffer offer) {
        return DispatchOfferRes.builder()
                .offerId(offer.getId())
                .dispatchId(offer.getDispatch().getId())
                .transporterId(offer.getTransporter().getId())
                .transporterName(offer.getTransporter().getName())
                .status(offer.getStatus())
                .sequence(offer.getSequence())
                .offeredAt(offer.getOfferedAt())
                .respondedAt(offer.getRespondedAt())
                .build();
    }
}
