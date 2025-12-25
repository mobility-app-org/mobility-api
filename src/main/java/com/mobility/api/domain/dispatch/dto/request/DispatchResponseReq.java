package com.mobility.api.domain.dispatch.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 배차 응답 요청 DTO
 * - 기사가 WebSocket으로 수락/거절할 때 사용
 */
public record DispatchResponseReq(
        @NotNull(message = "offerId는 필수입니다.")
        Long offerId
) {
}
