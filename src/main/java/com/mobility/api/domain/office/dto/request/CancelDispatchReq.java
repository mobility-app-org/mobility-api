package com.mobility.api.domain.office.dto.request;

public record CancelDispatchReq(
        String cancelReason // 취소 사유
) {
}
