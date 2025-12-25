package com.mobility.api.domain.dispatch.enums;

/**
 * 배차 제안 상태
 */
public enum OfferStatus {
    PENDING,    // 대기 중 (알림 전송 완료, 응답 대기)
    ACCEPTED,   // 수락
    REJECTED,   // 거절
    TIMEOUT     // 타임아웃 (5초 미응답)
}
