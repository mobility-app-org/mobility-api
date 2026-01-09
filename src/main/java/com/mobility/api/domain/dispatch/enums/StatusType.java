package com.mobility.api.domain.dispatch.enums;

public enum StatusType {
    HOLD,       // 자동배차 진행중 (주변 기사에게 순차 권유 중)
    OPEN,       // 모든 기사 확인 가능 (자동배차 대상 없거나 모두 거절)
    ASSIGNED,   // 배차 완료 (기사 배정됨)
    COMPLETED,  // 운송 완료
    CANCELED    // 취소
}
