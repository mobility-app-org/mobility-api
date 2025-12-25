package com.mobility.api.domain.dispatch.enums;

/**
 * 배차 제안 결과
 * - 타임아웃 처리 로직에서 사용
 */
public enum OfferResult {
    ACCEPTED,   // 수락됨
    REJECTED,   // 거절됨
    TIMEOUT     // 타임아웃 (5초 미응답)
}
